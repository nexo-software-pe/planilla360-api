package pe.nexosoftware.planilla360.boleta.almacenamiento;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Almacén en memoria para pruebas y para correr sin MinIO/R2; los archivos se pierden al reiniciar. */
public class MemoriaAlmacenArchivos implements AlmacenArchivos {

    private final Map<String, byte[]> archivos = new ConcurrentHashMap<>();

    @Override
    public void guardar(String clave, byte[] contenido, String tipoContenido) {
        archivos.put(clave, contenido.clone());
    }

    @Override
    public Optional<byte[]> leer(String clave) {
        return Optional.ofNullable(archivos.get(clave)).map(byte[]::clone);
    }
}
