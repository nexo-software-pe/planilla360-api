package pe.nexosoftware.planilla360.identidad.service;

import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.compartido.eventos.CambioRegistrado;
import pe.nexosoftware.planilla360.compartido.eventos.CambioRegistrado.Accion;
import pe.nexosoftware.planilla360.compartido.excepcion.AccesoProhibidoException;
import pe.nexosoftware.planilla360.compartido.excepcion.ConflictoException;
import pe.nexosoftware.planilla360.compartido.excepcion.RecursoNoEncontradoException;
import pe.nexosoftware.planilla360.compartido.excepcion.ReglaDeNegocioException;
import pe.nexosoftware.planilla360.compartido.paginacion.Pagina;
import pe.nexosoftware.planilla360.compartido.seguridad.Rol;
import pe.nexosoftware.planilla360.compartido.seguridad.UsuarioAutenticado;
import pe.nexosoftware.planilla360.empresa.EmpresaApi;
import pe.nexosoftware.planilla360.empresa.EmpresaResumen;
import pe.nexosoftware.planilla360.identidad.IdentidadApi;
import pe.nexosoftware.planilla360.identidad.dto.ActualizarUsuarioRequest;
import pe.nexosoftware.planilla360.identidad.dto.ResetPasswordRequest;
import pe.nexosoftware.planilla360.identidad.dto.UsuarioRequest;
import pe.nexosoftware.planilla360.identidad.dto.UsuarioResponse;
import pe.nexosoftware.planilla360.identidad.entity.Usuario;
import pe.nexosoftware.planilla360.identidad.mapper.UsuarioMapper;
import pe.nexosoftware.planilla360.identidad.repository.RefreshTokenRepository;
import pe.nexosoftware.planilla360.identidad.repository.UsuarioRepository;

/**
 * Administración de usuarios. Un administrador de empresa solo ve y modifica usuarios de su empresa;
 * el administrador de la plataforma puede operar sobre cualquier empresa indicando su identificador.
 */
@Service
@Transactional(readOnly = true)
public class UsuarioService implements IdentidadApi {

    private static final String ENTIDAD = "USUARIO";

    private final UsuarioRepository usuarios;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final EmpresaApi empresas;
    private final UsuarioMapper mapper;
    private final ApplicationEventPublisher eventos;

    public UsuarioService(
            UsuarioRepository usuarios,
            RefreshTokenRepository refreshTokens,
            PasswordEncoder encoder,
            EmpresaApi empresas,
            UsuarioMapper mapper,
            ApplicationEventPublisher eventos) {
        this.usuarios = usuarios;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.empresas = empresas;
        this.mapper = mapper;
        this.eventos = eventos;
    }

    @Override
    public Optional<UUID> empleadoIdDe(UUID usuarioId) {
        return usuarios.findById(usuarioId).map(Usuario::getEmpleadoId);
    }

    public Pagina<UsuarioResponse> listar(UUID tenantIdFiltro, Pageable pageable) {
        UsuarioAutenticado actor = UsuarioAutenticado.requerido();
        UUID tenant = actor.esAdminSaas() ? tenantIdFiltro : actor.tenantId();
        Page<Usuario> page = tenant == null ? usuarios.findAll(pageable) : usuarios.findByTenantId(tenant, pageable);
        return Pagina.de(page, mapper::aResponse);
    }

    public UsuarioResponse obtener(UUID id) {
        return mapper.aResponse(buscar(id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        UsuarioAutenticado actor = UsuarioAutenticado.requerido();
        if (request.rol() == Rol.ADMIN_SAAS) {
            throw new ReglaDeNegocioException("No se pueden crear administradores de la plataforma desde la API.");
        }
        UUID tenant = resolverTenant(actor, request.tenantId());
        EmpresaResumen empresa = empresas.buscarPorId(tenant)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la empresa indicada."));
        if (!empresa.activa()) {
            throw new ReglaDeNegocioException("La empresa está inactiva.");
        }
        if (usuarios.existsByEmailIgnoreCase(request.email().trim())) {
            throw new ConflictoException("Ya existe un usuario con ese correo.");
        }
        Usuario usuario = usuarios.saveAndFlush(new Usuario(
                tenant, request.email(), request.nombre(), encoder.encode(request.password()),
                request.rol(), request.empleadoId()));
        UsuarioResponse respuesta = mapper.aResponse(usuario);
        publicar(usuario, Accion.CREAR, null, respuesta);
        return respuesta;
    }

    @Transactional
    public UsuarioResponse actualizar(UUID id, ActualizarUsuarioRequest request) {
        if (request.rol() == Rol.ADMIN_SAAS) {
            throw new ReglaDeNegocioException("No se puede asignar el rol de administrador de la plataforma.");
        }
        Usuario usuario = buscar(id);
        UsuarioResponse antes = mapper.aResponse(usuario);
        usuario.actualizar(request.nombre(), request.rol(), request.empleadoId());
        usuarios.flush();
        UsuarioResponse despues = mapper.aResponse(usuario);
        publicar(usuario, Accion.ACTUALIZAR, antes, despues);
        return despues;
    }

    @Transactional
    public UsuarioResponse cambiarEstado(UUID id, boolean activo) {
        UsuarioAutenticado actor = UsuarioAutenticado.requerido();
        Usuario usuario = buscar(id);
        if (!activo && usuario.getId().equals(actor.usuarioId())) {
            throw new ReglaDeNegocioException("No puedes desactivar tu propio usuario.");
        }
        UsuarioResponse antes = mapper.aResponse(usuario);
        usuario.cambiarEstado(activo);
        if (!activo) {
            refreshTokens.revocarTodosDelUsuario(usuario.getId());
        }
        usuarios.flush();
        UsuarioResponse despues = mapper.aResponse(usuario);
        publicar(usuario, Accion.ACTUALIZAR, antes, despues);
        return despues;
    }

    @Transactional
    public void resetearPassword(UUID id, ResetPasswordRequest request) {
        Usuario usuario = buscar(id);
        usuario.cambiarPassword(encoder.encode(request.passwordNueva()));
        refreshTokens.revocarTodosDelUsuario(usuario.getId());
        UsuarioResponse snapshot = mapper.aResponse(usuario);
        publicar(usuario, Accion.ACTUALIZAR, snapshot, snapshot);
    }

    private UUID resolverTenant(UsuarioAutenticado actor, UUID tenantSolicitado) {
        if (actor.esAdminSaas()) {
            if (tenantSolicitado == null) {
                throw new ReglaDeNegocioException("Debes indicar la empresa (tenantId) del nuevo usuario.");
            }
            return tenantSolicitado;
        }
        if (tenantSolicitado != null && !tenantSolicitado.equals(actor.tenantId())) {
            throw new AccesoProhibidoException("No puedes crear usuarios en otra empresa.");
        }
        return actor.tenantId();
    }

    /** Un usuario de otra empresa se trata como inexistente: no se revela que existe. */
    private Usuario buscar(UUID id) {
        UsuarioAutenticado actor = UsuarioAutenticado.requerido();
        var encontrado = actor.esAdminSaas() ? usuarios.findById(id) : usuarios.findByIdAndTenantId(id, actor.tenantId());
        return encontrado.orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario solicitado."));
    }

    private void publicar(Usuario usuario, Accion accion, Object antes, Object despues) {
        eventos.publishEvent(new CambioRegistrado(ENTIDAD, usuario.getId(), usuario.getTenantId(), accion, antes, despues));
    }
}
