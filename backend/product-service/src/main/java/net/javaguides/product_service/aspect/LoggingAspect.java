package net.javaguides.product_service.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // log before service methods run
    @Before("execution(* net.javaguides.product_service.service.impl.*.*(..))")
    public void logBeforeMethod(JoinPoint joinPoint) {
        log.info("Starting method: {}", joinPoint.getSignature().toShortString());
    }

    // log after service methods complete
    @AfterReturning(
            pointcut = "execution(* net.javaguides.product_service.service.impl.*.*(..))",
            returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        log.info("Completed method: {} with result: {}",
                joinPoint.getSignature().toShortString(),
                result);
    }

    // log when an exception is thrown
    @AfterThrowing(
            pointcut = "execution(* net.javaguides.product_service.service.impl.*.*(..))",
            throwing = "error")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable error) {
        log.error("Exception in method: {}",
                joinPoint.getSignature().toShortString(),
                error);
    }
}
