package net.javaguides.identity_service.audit;


import lombok.RequiredArgsConstructor;
import net.javaguides.identity_service.entity.AuditLog;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {
    private final AuditLogService auditLogService;
    private final ExpressionParser parser = new SpelExpressionParser();

//    @AfterReturning("@annotation(audit)")
//    public void logAudit(JoinPoint joinPoint, Audit audit) {
//
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth == null || !auth.isAuthenticated()) return;
//
//        String username = auth.getName();
//        String action = audit.action();
//        String resource = resolveResource(audit.resource(), joinPoint);
//
//        auditLogService.save(username, action, resource);
//    }

    private String resolveResource(String spel, JoinPoint joinPoint) {
        if (spel == null || spel.isBlank()) return null;

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        EvaluationContext context = new StandardEvaluationContext();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = signature.getParameterNames();

        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        return parser.parseExpression(spel).getValue(context, String.class);
    }

    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {
        Object resourceId = null;
        //String resourceName = "user";
        // Nếu có class DTO, tìm parameter phù hợp
        if (!audit.resourceClass().equals(void.class)) {
            for (Object arg : joinPoint.getArgs()) {
                if (arg != null && audit.resourceClass().isAssignableFrom(arg.getClass())) {
                    // Lấy field id bằng Reflection
                    Field field = arg.getClass().getDeclaredField(audit.idField());
                    field.setAccessible(true);
                    resourceId = field.get(arg);
                    //resourceName = audit.resourceClass().getSimpleName();
                   // resourceName = "user";
                    break;
                }
            }
        }

        // 2️⃣ Trường hợp primitive / Long / String (không dùng resourceClass)
        if (resourceId == null && joinPoint.getArgs().length == 1) {
            Object arg = joinPoint.getArgs()[0];
            if (arg instanceof Number || arg instanceof String) {
                resourceId = arg;
                //resourceName = "user"; // hoặc mặc định theo action
            }
        }

        // Log hoặc lưu audit
        String resource =  "user: " + resourceId;
        System.out.println("AUDIT " + audit.action() + " " + resource);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;

        // 3️⃣ Lưu audit
//        AuditLog log = new AuditLog();
//        log.setUsername(auth.getName());
//        log.setAction(audit.action());
//        log.setResource(resource);
       // log.setSnapshot(convertToJson(snapshot)); // Jackson / Gson
        auditLogService.save(auth.getName(), audit.action(), resource);

        // Tiếp tục thực hiện method
        return joinPoint.proceed();
    }
}
