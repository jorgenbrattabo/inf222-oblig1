package inf222.aop.account.aspect;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import inf222.aop.account.Account;
import inf222.aop.account.annotation.Transfer;

@Aspect
public class TransferAspect {

    @Around("@annotation(transferAnnotation)")
    public Object aroundTransfer(ProceedingJoinPoint jp, Transfer transferAnnotation) throws Throwable {

        Logger logger = LoggerFactory.getLogger(jp.getTarget().getClass());
        Object[] args = jp.getArgs();

        MethodSignature signature = (MethodSignature) jp.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getName();
        String[] methodParams = signature.getParameterNames();

        Object result = null;
        boolean success = false;

        try {
            result = jp.proceed();
            success = (result instanceof Boolean) && ((Boolean) result);

            // Log international transfers (INFO)
            if (transferAnnotation.internationalTransfer()) {
                logger.info(logInternationalTransfer(args));
            }

            // Log transfers above threshold (INFO)
            double amount = (Double) args[2];
            if (amount > transferAnnotation.LogTransferAbove()) {
                logger.info(logTransferAbove(args, transferAnnotation.LogTransferAbove()));
            }

            // Log errors (ERROR) if the transfer failed
            if (!success && transferAnnotation.logErrors()) {
                logger.error(logErrors(args, methodName, methodParams));
            }

            return result;

        } catch (Throwable ex) {
            // Log errors (ERROR) on exception
            if (transferAnnotation.logErrors()) {
                logger.error(logErrors(args, methodName, methodParams));
            }
            throw ex;
        }
    }

    private String logInternationalTransfer(Object[] args) {
        Account from = (Account) args[0];
        Account to = (Account) args[1];
        Double amount = (Double) args[2];

        return String.format(
                "International transfer from %s to %s, %s %s converted to %s",
                from.getAccountName(),
                to.getAccountName(),
                amount,
                from.getCurrency(),
                to.getCurrency()
        );
    }

    private String logTransferAbove(Object[] args, double threshold) {
        Account from = (Account) args[0];
        Account to = (Account) args[1];
        Double amount = (Double) args[2];

        return String.format(
                "Transfer above %s from %s to %s, amount: %s",
                threshold,
                from.getAccountName(),
                to.getAccountName(),
                amount
        );
    }

    private String logErrors(Object[] args, String methodName, String[] methodParams) {
        Account from = (Account) args[0];
        Account to = (Account) args[1];
        Double amount = (Double) args[2];

        return String.format(
                "Error in transfer from %s to %s, amount: %s %s, method: %s(%s)",
                from.getAccountName(),
                to.getAccountName(),
                amount,
                from.getCurrency(),
                methodName,
                String.join(", ", methodParams)
        );
    }
}