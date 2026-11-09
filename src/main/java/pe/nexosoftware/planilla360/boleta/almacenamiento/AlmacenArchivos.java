package pe.nexosoftware.planilla360.boleta.almacenamiento;

import java.util.Optional;

/** Puerto de salida: guarda y recupera archivos por clave (MinIO en local, Cloudflare R2 en la nube). */
public interface AlmacenArchivos {

    void guardar(String clave, byte[] contenido, String tipoContenido);

    Optional<byte[]> leer(String clave);
}
