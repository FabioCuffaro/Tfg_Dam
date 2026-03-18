package com.tfg.tfg_redsocial.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * CloudinaryService - Gestiona la subida de imágenes a Cloudinary
 *
 * ¿Por qué subir desde el backend y no directamente desde el frontend?
 *
 * Si subiéramos desde el frontend con la API Key y Secret en el JS,
 * cualquiera que abra las DevTools del navegador vería esas credenciales
 * y podría subir archivos a nuestra cuenta de Cloudinary sin límite.
 *
 * Al hacerlo desde el backend:
 *   1. Las credenciales solo viven en application.properties (servidor)
 *   2. Podemos validar el archivo antes de subirlo (tipo, tamaño)
 *   3. Podemos organizar las imágenes en carpetas (prometeo/posts)
 *
 * @Service → Spring gestiona esta clase como un bean singleton.
 * Se crea una sola vez al arrancar la app y se reutiliza en cada petición.
 */
@Service
public class CloudinaryService {

    // El objeto Cloudinary es thread-safe: podemos compartirlo entre peticiones
    private final Cloudinary cloudinary;

    /**
     * Constructor: Spring inyecta las 3 propiedades de application.properties.
     *
     * @Value("${cloudinary.cloud-name}") lee el valor de la propiedad
     * "cloudinary.cloud-name" del fichero application.properties.
     *
     * ObjectUtils.asMap() construye el mapa de configuración que
     * el SDK de Cloudinary necesita para autenticarse.
     */
    public CloudinaryService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}")    String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {

        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key",    apiKey,
                "api_secret", apiSecret
        ));
    }

    // =================================================================
    // MÉTODO PRINCIPAL: subir imagen
    // =================================================================

    /**
     * Sube un archivo de imagen a Cloudinary y devuelve su URL pública.
     *
     * El flujo completo:
     *   1. MultipartFile llega desde el PostController
     *   2. .getBytes() extrae los bytes del archivo en memoria
     *   3. cloudinary.uploader().upload() los envía a la API de Cloudinary
     *   4. Cloudinary devuelve un Map con metadatos: public_id, secure_url, width...
     *   5. Extraemos "secure_url" (URL HTTPS) y la devolvemos
     *   6. PostService guarda esa URL en el campo image_url del post
     *
     * @param file   El archivo que llega en el formulario multipart
     * @return       La URL pública HTTPS de la imagen en Cloudinary
     */
    public String uploadImage(MultipartFile file) {
        try {
            // Validar que sea realmente una imagen antes de subir
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new RuntimeException("El archivo debe ser una imagen (jpg, png, gif, webp)");
            }

            // ObjectUtils.asMap() construye las opciones de la subida:
            //   folder       → carpeta donde se guarda en Cloudinary
            //   resource_type → "image" para que Cloudinary la procese como imagen
            Map result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder",        "prometeo/posts",
                            "resource_type", "image"
                    )
            );

            // secure_url → URL HTTPS de la imagen ya procesada y almacenada
            return (String) result.get("secure_url");

        } catch (IOException e) {
            // IOException si falla la lectura del archivo o la conexión con Cloudinary
            throw new RuntimeException("Error al subir la imagen: " + e.getMessage());
        }
    }
}
