package pe.nexosoftware.planilla360.notificacion.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import pe.nexosoftware.planilla360.boleta.BoletaApi;
import pe.nexosoftware.planilla360.compartido.excepcion.RecursoNoEncontradoException;
import pe.nexosoftware.planilla360.compartido.tenant.TenantContext;
import pe.nexosoftware.planilla360.empleado.EmpleadoApi;
import pe.nexosoftware.planilla360.empleado.FichaEmpleado;
import pe.nexosoftware.planilla360.empresa.EmpresaApi;
import pe.nexosoftware.planilla360.notificacion.mensajeria.CorreoBoleta;
import pe.nexosoftware.planilla360.notificacion.plantilla.PlantillaCorreo;

/**
 * Envía el aviso de boleta disponible. Si el envío falla lanza la excepción (el consumidor reintenta); si el
 * trabajador no tiene correo o los correos están apagados, lo registra y sigue (no hay nada que reintentar).
 */
@Service
public class EnvioCorreoService {

    private static final Logger LOG = LoggerFactory.getLogger(EnvioCorreoService.class);

    private final ObjectProvider<JavaMailSender> correo;
    private final EmpleadoApi empleados;
    private final EmpresaApi empresas;
    private final BoletaApi boletas;
    private final boolean habilitado;
    private final String remitente;
    private final PlantillaCorreo plantilla = PlantillaCorreo.cargar("templates/correo/boleta.html");

    public EnvioCorreoService(
            ObjectProvider<JavaMailSender> correo,
            EmpleadoApi empleados,
            EmpresaApi empresas,
            BoletaApi boletas,
            @Value("${app.notificaciones.habilitadas:false}") boolean habilitado,
            @Value("${app.notificaciones.remitente:no-responder@planilla360.ejemplo.pe}") String remitente) {
        this.correo = correo;
        this.empleados = empleados;
        this.empresas = empresas;
        this.boletas = boletas;
        this.habilitado = habilitado;
        this.remitente = remitente;
    }

    public void enviarAvisoDeBoleta(CorreoBoleta aviso) {
        JavaMailSender enviador = correo.getIfAvailable();
        if (!habilitado || enviador == null) {
            LOG.info("Correos apagados: no se avisa de la boleta {}", aviso.boletaId());
            return;
        }
        FichaEmpleado ficha = empleados.fichaDe(aviso.empleadoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el trabajador del aviso."));
        if (ficha.email() == null || ficha.email().isBlank()) {
            LOG.warn("El trabajador {} no tiene correo: no se avisa de la boleta {}", ficha.id(), aviso.boletaId());
            return;
        }
        String empresa = empresas.buscarPorId(TenantContext.requerido()).map(e -> e.razonSocial()).orElse("Planilla360");
        String documento = "GRATIFICACION".equals(aviso.tipo()) ? "boleta de gratificación"
                : "CTS".equals(aviso.tipo()) ? "constancia de depósito de CTS" : "boleta de pago";
        String html = plantilla.renderizar(Map.of(
                "empresa", empresa,
                "nombre", ficha.nombreCompleto(),
                "documento", documento,
                "periodo", aviso.periodo()));
        try {
            MimeMessage mensaje = enviador.createMimeMessage();
            MimeMessageHelper ayudante = new MimeMessageHelper(mensaje, false, StandardCharsets.UTF_8.name());
            ayudante.setFrom(remitente);
            ayudante.setTo(ficha.email());
            ayudante.setSubject("Tu " + documento + " de " + aviso.periodo() + " está disponible");
            ayudante.setText(html, true);
            enviador.send(mensaje);
        } catch (MessagingException e) {
            throw new IllegalStateException("No se pudo armar el correo", e);
        }
        boletas.marcarEnviada(aviso.boletaId());
    }
}
