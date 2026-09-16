package com.example.template.global.storage;

import com.example.template.global.config.AppProperties;
import com.example.template.global.error.BusinessException;
import com.example.template.global.error.CommonErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 로컬 디스크 저장 구현.
 *
 * <p>인스턴스를 여러 개 띄울 때는 모두 같은 볼륨을 마운트해야 한다(PRD SCALE-02).
 */
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path root;

    public LocalStorageService(AppProperties appProperties) {
        this.root =
                Path.of(appProperties.storage().localPath()).toAbsolutePath().normalize();
    }

    @Override
    public void store(String key, InputStream content, String contentType) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR, "파일 저장에 실패했습니다.");
        }
    }

    @Override
    public InputStream load(String key) {
        Path target = resolve(key);
        try {
            return Files.newInputStream(target);
        } catch (IOException e) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "파일을 찾을 수 없습니다.");
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            // 삭제 실패는 치명적이지 않다. 다음 정리 작업에서 다시 시도한다.
            log.warn("파일 삭제 실패: key={}", key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(resolve(key));
    }

    /** 경로 조작(../)으로 저장소 밖 파일에 접근하는 것을 막는다. */
    private Path resolve(String key) {
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT, "잘못된 파일 경로입니다.");
        }
        return target;
    }
}
