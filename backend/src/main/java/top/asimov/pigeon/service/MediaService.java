package top.asimov.pigeon.service;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import top.asimov.pigeon.config.MediaPathProperties;
import top.asimov.pigeon.config.StorageProperties;
import top.asimov.pigeon.exception.BusinessException;
import top.asimov.pigeon.handler.MediaFileResourceHandler;
import top.asimov.pigeon.mapper.EpisodeMapper;
import top.asimov.pigeon.model.dto.SubtitleInfo;
import top.asimov.pigeon.model.entity.Episode;
import top.asimov.pigeon.model.enums.EpisodeStatus;
import top.asimov.pigeon.service.storage.S3StorageService;
import top.asimov.pigeon.util.MediaKeyUtil;

@Slf4j
@Service
public class MediaService {

  private final EpisodeMapper episodeMapper;
  private final MessageSource messageSource;
  private final StorageProperties storageProperties;
  private final S3StorageService s3StorageService;
  private final MediaPathProperties mediaPathProperties;
  private final MediaFileResourceHandler mediaFileResourceHandler;

  public MediaService(EpisodeMapper episodeMapper, MessageSource messageSource, StorageProperties storageProperties,
      S3StorageService s3StorageService, MediaPathProperties mediaPathProperties,
      MediaFileResourceHandler mediaFileResourceHandler) {
    this.episodeMapper = episodeMapper;
    this.messageSource = messageSource;
    this.storageProperties = storageProperties;
    this.s3StorageService = s3StorageService;
    this.mediaPathProperties = mediaPathProperties;
    this.mediaFileResourceHandler = mediaFileResourceHandler;
  }

  public boolean isS3ModeEnabled() {
    return storageProperties.isS3Mode();
  }

  public boolean objectKeyExists(String key) {
    if (!StringUtils.hasText(key) || !isS3ModeEnabled()) {
      return false;
    }
    return s3StorageService.keyExists(key);
  }

  public boolean isFilePathAllowed(File file) {
    if (file == null) {
      return false;
    }
    return !isFileInAllowedDirectory(file);
  }

  public String saveFeedCover(String feedId, MultipartFile file) throws IOException {
    String contentType = file.getContentType();
    if (!Arrays.asList("image/jpeg", "image/png", "image/webp").contains(contentType)) {
      throw new IOException("Invalid file type. Only JPG, JPEG, PNG, and WEBP are allowed.");
    }
