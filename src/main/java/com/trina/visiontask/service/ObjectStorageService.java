package com.trina.visiontask.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.*;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;


@Service
public class ObjectStorageService {
    private static final Logger log = LoggerFactory.getLogger(ObjectStorageService.class);
    @Getter
    private final ObjectStorageOptions options;
    private final OSS ossClient;
    private final DataBufferFactory dataBufferFactory;

    public ObjectStorageService(
            @Qualifier("dataBufferFactory") DataBufferFactory dataBufferFactory,
            @Qualifier("ossClient") OSS ossClient,
            @Qualifier("objectStorageOptions") ObjectStorageOptions objectStorageOptions) {
        this.dataBufferFactory = dataBufferFactory;
        this.ossClient = ossClient;
        this.options = objectStorageOptions;
    }

    /**
     * 将DataBuffer流以分片方式上传到OSS
     */
    public Mono<CompleteMultipartUploadResult> uploadFlux(String ossFileName, Flux<DataBuffer> dataBufferFlux) {
        // 初始化分片上传
        return Mono.fromCallable(() -> initializeMultipartUpload(ossFileName))
                .flatMap(uploadResult -> {
                    // 按分片大小分割数据流
                    return dataBufferFlux
                            .window(options.getPartSize())
                            .index()
                            .flatMapSequential(pair -> {
                                long partNumber = pair.getT1() + 1; // 分片编号从1开始
                                Flux<DataBuffer> partFlux = pair.getT2();

                                // 处理每个分片
                                return partFlux
                                        .collectList()
                                        .publishOn(Schedulers.boundedElastic())
                                        .flatMap(dataBuffers -> {
                                            try {
                                                // 合并DataBuffer为字节数组
                                                byte[] partData = mergeDataBuffers(dataBuffers);
                                                // 上传分片
                                                return uploadPart(ossFileName, uploadResult.getUploadId(), partNumber, partData);
                                            } finally {
                                                // 确保缓冲区被释放，即使发生错误
                                                dataBuffers.forEach(DataBufferUtils::release);
                                            }
                                        });
                            })
                            .collectList()
                            .publishOn(Schedulers.boundedElastic())
                            .flatMap(parts -> completeMultipartUpload(ossFileName, uploadResult.getUploadId(), parts))
                            .onErrorResume(e -> {
                                // 发生错误时取消分片上传
                                abortMultipartUpload(ossFileName, uploadResult.getUploadId());
                                return Mono.error(e);
                            });
                });
    }

    /**
     * 初始化分片上传
     */
    private InitiateMultipartUploadResult initializeMultipartUpload(String fileName) {
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(options.getBucketName(), fileName);
        return ossClient.initiateMultipartUpload(request);
    }

    /**
     * 上传单个分片
     */
    private Mono<PartETag> uploadPart(String fileName, String uploadId, long partNumber, byte[] data) {
        return Mono.fromCallable(() -> {
            UploadPartRequest uploadPartRequest = new UploadPartRequest();
            uploadPartRequest.setBucketName(options.getBucketName());
            uploadPartRequest.setKey(fileName);
            uploadPartRequest.setUploadId(uploadId);
            uploadPartRequest.setPartNumber((int) partNumber);
            uploadPartRequest.setInputStream(new ByteArrayInputStream(data));
            uploadPartRequest.setPartSize(data.length);

            UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
            return uploadPartResult.getPartETag();
        });
    }

    /**
     * 完成分片上传
     */
    private Mono<CompleteMultipartUploadResult> completeMultipartUpload(String fileName, String uploadId, List<PartETag> parts) {
        return Mono.fromCallable(() -> {
            CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest(
                    options.getBucketName(), fileName, uploadId, parts);
            return ossClient.completeMultipartUpload(completeRequest);
        });
    }

    /**
     * 取消分片上传
     */
    private void abortMultipartUpload(String fileName, String uploadId) {
        try {
            AbortMultipartUploadRequest abortRequest = new AbortMultipartUploadRequest(
                    options.getBucketName(), fileName, uploadId);
            ossClient.abortMultipartUpload(abortRequest);
        } catch (Exception e) {
            // 记录取消上传时的异常
            log.error("abort multipart upload failed. {}", e.getMessage());
        }
    }

    /**
     * 合并DataBuffer列表为字节数组
     */
    private byte[] mergeDataBuffers(List<DataBuffer> dataBuffers) {
        int totalLength = dataBuffers.stream()
                .mapToInt(DataBuffer::readableByteCount)
                .sum();

        byte[] result = new byte[totalLength];
        int position = 0;

        for (DataBuffer buffer : dataBuffers) {
            int bytesToCopy = buffer.readableByteCount();
            buffer.read(result, position, bytesToCopy);
            position += bytesToCopy;
            // 这里不直接释放，在finally块中统一释放
        }

        return result;
    }

    /**
     * 生成唯一的文件名
     */
    private String generateUniqueFileName(String originalFileName) {
        String extension = "";
        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = originalFileName.substring(lastDotIndex);
        }
        return UUID.randomUUID() + extension;
    }

    /**
     * 从OSS下载文件并返回InputStream
     * 适合需要传统IO流处理的场景
     *
     * @param ossFileName OSS中的文件名
     * @return 文件输入流
     */
    public Mono<OSSObject> download(String ossFileName) {
        // 异步执行下载操作，避免阻塞
        return Mono.fromCallable(() -> {
            // 检查文件是否存在
            if (!ossClient.doesObjectExist(options.getBucketName(), ossFileName)) {
                throw new IllegalArgumentException(String.format("file %s do no exist", ossFileName));
            }

            // 获取OSS文件输入流
            GetObjectRequest request = new GetObjectRequest(options.getBucketName(), ossFileName);
            return ossClient.getObject(request);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 从OSS下载文件并返回异步数据流
     * 适合流式处理大文件的场景
     *
     * @param ossFileName OSS中的文件名
     * @return 文件数据的异步流
     */
    public Flux<DataBuffer> downloadFlux(String ossFileName) {
        return Mono.fromCallable(() -> {
                    // 检查文件是否存在
                    if (!ossClient.doesObjectExist(options.getBucketName(), ossFileName)) {
                        throw new IllegalArgumentException(String.format("file %s do no exist", ossFileName));
                    }

                    // 获取OSS文件输入流
                    GetObjectRequest request = new GetObjectRequest(options.getBucketName(), ossFileName);
                    OSSObject ossObject = ossClient.getObject(request);
                    return ossObject.getObjectContent();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(inputStream ->
                        DataBufferUtils.readInputStream(
                                        () -> inputStream,
                                        dataBufferFactory,
                                        options.getPartSize())
                                .publishOn(Schedulers.boundedElastic())
                                .doFinally(signalType -> {
                                    try {
                                        inputStream.close();
                                    } catch (Exception e) {
                                        log.error("error closing input stream: {}", e.getMessage());
                                    }
                                })
                );
    }

    /**
     * 上传InputStream到OSS
     *
     * @param ossFileName OSS中的文件名
     * @param inputStream 文件输入流
     * @return 上传结果
     */
    public Mono<CompleteMultipartUploadResult> upload(String ossFileName, InputStream inputStream) {
        // 参数校验
        if (ossFileName == null || ossFileName.isEmpty()) {
            return Mono.error(new IllegalArgumentException("ossFileName cannot be null or empty"));
        }
        if (inputStream == null) {
            return Mono.error(new IllegalArgumentException("inputStream cannot be null"));
        }

        Flux<DataBuffer> dataBufferFlux = DataBufferUtils.readInputStream(
                        () -> inputStream, dataBufferFactory, options.getPartSize())
                .publishOn(Schedulers.boundedElastic())
                .doOnError(throwable -> {
                    // 异常处理时尝试关闭流
                    try {
                        inputStream.close();
                    } catch (Exception ex) {
                        log.error("error closing input stream: {}", ex.getMessage());
                    }
                })
                .publishOn(Schedulers.boundedElastic())
                .doFinally(signalType -> {
                    // 确保流被关闭
                    try {
                        inputStream.close();
                    } catch (Exception ex) {
                        log.error("error closing input stream: {}", ex.getMessage());
                    }
                });

        return uploadFlux(ossFileName, dataBufferFlux);
    }

    public Mono<List<OSSObjectSummary>> listObjects(String prefix, Integer maxKeys) {
        return Mono.fromCallable(() -> {
            ListObjectsRequest request = new ListObjectsRequest();
            request.setBucketName(options.getBucketName());
            if (prefix != null && !prefix.isEmpty()) {
                if (!prefix.endsWith("/")) {
                    request.setPrefix(prefix + "/");
                }
                request.setPrefix(prefix);
            }

            if (maxKeys != null && maxKeys > 0) {
                request.setMaxKeys(maxKeys);
            }

            ObjectListing objectListing = ossClient.listObjects(request);
            return objectListing.getObjectSummaries();
        }).subscribeOn(Schedulers.boundedElastic());
    }


    /**
     * 分页列出存储桶中的对象
     *
     * @param prefix  对象名称前缀过滤器
     * @param maxKeys 最大返回对象数量
     * @param marker  分页标记
     * @return 对象列表结果（包含分页信息）
     */
    public Mono<ObjectListing> listObjectsWithMarker(String prefix, Integer maxKeys, String marker) {
        return Mono.fromCallable(() -> {
            ListObjectsRequest request = new ListObjectsRequest();
            request.setBucketName(options.getBucketName());

            if (prefix != null && !prefix.isEmpty()) {
                request.setPrefix(prefix);
            }

            if (maxKeys != null && maxKeys > 0) {
                request.setMaxKeys(maxKeys);
            }

            if (marker != null && !marker.isEmpty()) {
                request.setMarker(marker);
            }

            return ossClient.listObjects(request);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 批量创建多级目录
     *
     * @param directoryPath 完整目录路径
     * @return 创建结果列表
     */
    public Mono<List<PutObjectResult>> createDirectories(String directoryPath) {
        return Mono.fromCallable(() -> {
            List<PutObjectResult> results = new java.util.ArrayList<>();

            // 确保路径以 "/" 结尾
            String fullPath = directoryPath;
            if (!fullPath.endsWith("/")) {
                fullPath = fullPath + "/";
            }

            // 分割路径并逐级创建目录
            String[] parts = fullPath.split("/");
            StringBuilder currentPath = new StringBuilder();

            for (String part : parts) {
                if (!part.isEmpty()) {
                    currentPath.append(part).append("/");

                    // 创建当前层级目录
                    PutObjectRequest request = new PutObjectRequest(
                            options.getBucketName(),
                            currentPath.toString(),
                            new ByteArrayInputStream(new byte[0]),
                            new ObjectMetadata()
                    );

                    ObjectMetadata metadata = new ObjectMetadata();
                    metadata.setContentLength(0);
                    request.setMetadata(metadata);

                    PutObjectResult result = ossClient.putObject(request);
                    results.add(result);
                }
            }

            return results;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public void listDirectoryObjects(String prefix) throws Exception {

    }
}
