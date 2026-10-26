package org.devlive.metaforge.fs;

import lombok.extern.slf4j.Slf4j;

import java.nio.file.Paths;

@Slf4j
public class LocalFsService
        implements FsService
{
    @Override
    public FsResponse writer(FsRequest request)
    {
        log.info("LocalFs writer origin path [ {} ]", request.getLocalPath());
        String targetPath = Paths.get(
                request.getEndpoint() != null ? request.getEndpoint() : "",
                request.getBucket() != null ? request.getBucket() : "",
                request.getFileName()
        ).toString();

        FsResponse response = FsResponse.builder()
                .origin(request.getLocalPath())
                .remote(targetPath)
                .successful(true)
                .build();
        log.info("LocalFs writer target path [ {} ]", targetPath);
        try {
            if (request.getLocalPath() == null || request.getLocalPath().isEmpty()) {
                IOUtils.copy(request.getStream(), targetPath, true);
            }
            else {
                IOUtils.copy(request.getLocalPath(), targetPath, true);
            }
            log.info("LocalFs writer [ {} ] successfully", targetPath);
        }
        catch (Exception e) {
            log.error("LocalFs writer error", e);
            response.setSuccessful(false);
            response.setMessage(e.getMessage());
        }
        return response;
    }

    @Override
    public FsResponse reader(FsRequest request)
    {
        String targetPath = Paths.get(
                request.getEndpoint() != null ? request.getEndpoint() : "",
                request.getBucket() != null ? request.getBucket() : "",
                request.getFileName()
        ).toString();
        log.info("LocalFs reader origin path [ {} ]", targetPath);
        FsResponse response = FsResponse.builder()
                .remote(targetPath)
                .successful(true)
                .build();
        try {
            response.setContext(IOUtils.reader(targetPath));
            log.info("LocalFs reader [ {} ] successfully", targetPath);
        }
        catch (Exception e) {
            log.error("LocalFs reader error", e);
            response.setSuccessful(false);
            response.setMessage(e.getMessage());
        }
        return response;
    }

    @Override
    public FsResponse delete(FsRequest request)
    {
        String targetPath = Paths.get(
                request.getEndpoint() != null ? request.getEndpoint() : "",
                request.getBucket() != null ? request.getBucket() : "",
                request.getFileName()
        ).toString();
        log.info("LocalFs delete origin path [ {} ]", targetPath);
        try {
            boolean status = IOUtils.delete(targetPath);
            log.info("LocalFs delete [ {} ] successfully", targetPath);
            return FsResponse.builder()
                    .successful(status)
                    .build();
        }
        catch (Exception e) {
            log.error("LocalFs delete error", e);
            return FsResponse.builder()
                    .successful(false)
                    .message(e.getMessage())
                    .build();
        }
    }
}
