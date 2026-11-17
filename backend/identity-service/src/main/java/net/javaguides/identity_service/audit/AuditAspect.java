package net.javaguides.identity_service.audit;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * Writes an audit log after a method annotated with {@link Audit} succeeds.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogService auditLogService;

    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {
        // run the method first: failed actions are not audited
        Object result = joinPoint.proceed();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return result;
        }

        String resource = "user: " + resolveResourceId(joinPoint, audit);
        LOGGER.info("AUDIT {} {} by {}", audit.action(), resource, auth.getName());
        auditLogService.save(auth.getName(), audit.action(), resource);
        return result;
    }

    /**
     * Resource id: the idField of the resourceClass argument if configured, otherwise the first numeric argument.
     */
    private static Object resolveResourceId(ProceedingJoinPoint joinPoint, Audit audit) throws ReflectiveOperationException {
        if (!audit.resourceClass().equals(void.class)) {
            for (Object arg : joinPoint.getArgs()) {
                if (arg != null && audit.resourceClass().isAssignableFrom(arg.getClass())) {
                    Field field = arg.getClass().getDeclaredField(audit.idField());
                    field.setAccessible(true);
                    return field.get(arg);
                }
            }
        }
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof Number) {
                return arg;
            }
        }
        return null;
    }
}
