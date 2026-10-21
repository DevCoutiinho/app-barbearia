package dev.barbershop.api.barberservice.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ImageStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png");
    private static final Map<String, String> MIME_TO_DEFAULT_EXTENSION = Map.of(
            "image/jpeg", ".jpg",
            "image/jpg", ".jpg",
            "image/pjpeg", ".jpg",
            "image/png", ".png",
            "image/x-png", ".png"
    );

    private final Path uploadDirectory =
            Paths.get("uploads").toAbsolutePath().normalize();

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("O arquivo de imagem não pode estar vazio.");
        }

        try {
            Files.createDirectories(uploadDirectory);

            String extension = getValidatedExtension(file);
            String filename = UUID.randomUUID() + extension;

            Path destination = uploadDirectory
                    .resolve(filename)
                    .normalize();

            if (!destination.startsWith(uploadDirectory)) {
                throw new IllegalArgumentException("Caminho de arquivo inválido.");
            }

            try (InputStream input = file.getInputStream()) {
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            }

            return filename;
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao armazenar o arquivo de imagem.", e);
        }
    }

    public void delete(String filename) {
        if (!StringUtils.hasText(filename)) {
            return;
        }

        try {
            Path filePath = uploadDirectory
                    .resolve(Paths.get(filename).getFileName().toString())
                    .normalize();

            if (!filePath.startsWith(uploadDirectory)) {
                throw new IllegalArgumentException("Caminho de arquivo inválido.");
            }

            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao excluir o arquivo de imagem.", e);
        }
    }

    private String getValidatedExtension(MultipartFile file) {
        String contentType = file.getContentType();

        if (contentType == null || !MIME_TO_DEFAULT_EXTENSION.containsKey(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Tipo de arquivo não suportado. Apenas JPG, JPEG e PNG são permitidos.");
        }

        String originalFilename = file.getOriginalFilename();

        if (StringUtils.hasText(originalFilename)) {
            String cleanFilename = StringUtils.cleanPath(originalFilename);
            int lastDotIndex = cleanFilename.lastIndexOf('.');

            if (lastDotIndex != -1) {
                String fileExt = cleanFilename.substring(lastDotIndex).toLowerCase(Locale.ROOT);
                if (ALLOWED_EXTENSIONS.contains(fileExt)) {
                    return fileExt;
                }
                throw new IllegalArgumentException("Extensão de arquivo não permitida. Apenas .jpg, .jpeg e .png são aceitas.");
            }
        }

        return MIME_TO_DEFAULT_EXTENSION.get(contentType.toLowerCase(Locale.ROOT));
    }
}
