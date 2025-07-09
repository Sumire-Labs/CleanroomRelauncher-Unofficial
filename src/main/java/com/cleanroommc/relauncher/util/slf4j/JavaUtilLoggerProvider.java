package com.cleanroommc.relauncher.util.slf4j;

import org.apache.logging.log4j.LogManager;
import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

public class JavaUtilLoggerProvider implements SLF4JServiceProvider {

    private ILoggerFactory loggerFactory;

    @Override
    public ILoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return null;
    }

    @Override
    public MDCAdapter getMDCAdapter() {
        return null;
    }

    @Override
    public String getRequestedApiVersion() {
        return "2.0.0";
    }

    @Override
    public void initialize() {
        this.loggerFactory = new JavaUtilLoggerFactory();
    }

    private static class JavaUtilLoggerFactory implements ILoggerFactory {

        @Override
        public Logger getLogger(String name) {
            return new JavaUtilLogger(LogManager.getLogger(name));
        }

    }

    private static class JavaUtilLogger implements Logger {

        private final org.apache.logging.log4j.Logger logger;

        JavaUtilLogger(org.apache.logging.log4j.Logger logger) {
            this.logger = logger;
        }

        @Override
        public String getName() {
            return this.logger.getName();
        }

        @Override
        public boolean isTraceEnabled() {
            return logger.isTraceEnabled();
        }

        @Override
        public void trace(String msg) {
            if (logger.isTraceEnabled()) {
                logger.trace(msg);
            }
        }

        @Override
        public void trace(String format, Object arg) {
            if (logger.isTraceEnabled()) {
                logger.trace(format(format, arg));
            }
        }

        @Override
        public void trace(String format, Object arg1, Object arg2) {
            if (logger.isTraceEnabled()) {
                logger.trace(format(format, arg1, arg2));
            }
        }

        @Override
        public void trace(String format, Object... arguments) {
            if (logger.isTraceEnabled()) {
                logger.trace(format(format, arguments));
            }
        }

        @Override
        public void trace(String msg, Throwable t) {
            if (logger.isTraceEnabled()) {
                logger.trace(msg, t);
            }
        }

        @Override
        public boolean isTraceEnabled(Marker marker) {
            return logger.isTraceEnabled();
        }

        @Override
        public void trace(Marker marker, String msg) {
            trace(msg);
        }

        @Override
        public void trace(Marker marker, String format, Object arg) {
            trace(format, arg);
        }

        @Override
        public void trace(Marker marker, String format, Object arg1, Object arg2) {
            trace(format, arg1, arg2);
        }

        @Override
        public void trace(Marker marker, String format, Object... arguments) {
            trace(format, arguments);
        }

        @Override
        public void trace(Marker marker, String msg, Throwable t) {
            trace(msg, t);
        }

        @Override
        public boolean isDebugEnabled() {
            return logger.isDebugEnabled();
        }

        @Override
        public void debug(String msg) {
            if (logger.isDebugEnabled()) {
                logger.debug(msg);
            }
        }

        @Override
        public void debug(String format, Object arg) {
            if (logger.isDebugEnabled()) {
                logger.debug(format(format, arg));
            }
        }

        @Override
        public void debug(String format, Object arg1, Object arg2) {
            if (logger.isDebugEnabled()) {
                logger.debug(format(format, arg1, arg2));
            }
        }

        @Override
        public void debug(String format, Object... arguments) {
            if (logger.isDebugEnabled()) {
                logger.debug(format(format, arguments));
            }
        }

        @Override
        public void debug(String msg, Throwable t) {
            if (logger.isDebugEnabled()) {
                logger.debug(msg, t);
            }
        }

        @Override
        public boolean isDebugEnabled(Marker marker) {
            return logger.isDebugEnabled();
        }

        @Override
        public void debug(Marker marker, String msg) {
            debug(msg);
        }

        @Override
        public void debug(Marker marker, String format, Object arg) {
            debug(format, arg);
        }

        @Override
        public void debug(Marker marker, String format, Object arg1, Object arg2) {
            debug(format, arg1, arg2);
        }

        @Override
        public void debug(Marker marker, String format, Object... arguments) {
            debug(format, arguments);
        }

        @Override
        public void debug(Marker marker, String msg, Throwable t) {
            debug(msg, t);
        }

        @Override
        public boolean isInfoEnabled() {
            return logger.isInfoEnabled();
        }

        @Override
        public void info(String msg) {
            if (logger.isInfoEnabled()) {
                logger.info(msg);
            }
        }

        @Override
        public void info(String format, Object arg) {
            if (logger.isInfoEnabled()) {
                logger.info(format(format, arg));
            }
        }

        @Override
        public void info(String format, Object arg1, Object arg2) {
            if (logger.isInfoEnabled()) {
                logger.info(format(format, arg1, arg2));
            }
        }

        @Override
        public void info(String format, Object... arguments) {
            if (logger.isInfoEnabled()) {
                logger.info(format(format, arguments));
            }
        }

        @Override
        public void info(String msg, Throwable t) {
            if (logger.isInfoEnabled()) {
                logger.info(msg, t);
            }
        }

        @Override
        public boolean isInfoEnabled(Marker marker) {
            return logger.isInfoEnabled();
        }

        @Override
        public void info(Marker marker, String msg) {
            info(msg);
        }

        @Override
        public void info(Marker marker, String format, Object arg) {
            info(format, arg);
        }

        @Override
        public void info(Marker marker, String format, Object arg1, Object arg2) {
            info(format, arg1, arg2);
        }

        @Override
        public void info(Marker marker, String format, Object... arguments) {
            info(format, arguments);
        }

        @Override
        public void info(Marker marker, String msg, Throwable t) {
            info(msg, t);
        }

        @Override
        public boolean isWarnEnabled() {
            return logger.isWarnEnabled();
        }

        @Override
        public void warn(String msg) {
            if (logger.isWarnEnabled()) {
                logger.warn(msg);
            }
        }

        @Override
        public void warn(String format, Object arg) {
            if (logger.isWarnEnabled()) {
                logger.warn(format(format, arg));
            }
        }

        @Override
        public void warn(String format, Object arg1, Object arg2) {
            if (logger.isWarnEnabled()) {
                logger.warn(format(format, arg1, arg2));
            }
        }

        @Override
        public void warn(String format, Object... arguments) {
            if (logger.isWarnEnabled()) {
                logger.warn(format(format, arguments));
            }
        }

        @Override
        public void warn(String msg, Throwable t) {
            if (logger.isWarnEnabled()) {
                logger.warn(msg, t);
            }
        }

        @Override
        public boolean isWarnEnabled(Marker marker) {
            return logger.isWarnEnabled();
        }

        @Override
        public void warn(Marker marker, String msg) {
            warn(msg);
        }

        @Override
        public void warn(Marker marker, String format, Object arg) {
            warn(format, arg);
        }

        @Override
        public void warn(Marker marker, String format, Object arg1, Object arg2) {
            warn(format, arg1, arg2);
        }

        @Override
        public void warn(Marker marker, String format, Object... arguments) {
            warn(format, arguments);
        }

        @Override
        public void warn(Marker marker, String msg, Throwable t) {
            warn(msg, t);
        }

        @Override
        public boolean isErrorEnabled() {
            return logger.isErrorEnabled();
        }

        @Override
        public void error(String msg) {
            if (logger.isErrorEnabled()) {
                logger.error(msg);
            }
        }

        @Override
        public void error(String format, Object arg) {
            if (logger.isErrorEnabled()) {
                logger.error(format(format, arg));
            }
        }

        @Override
        public void error(String format, Object arg1, Object arg2) {
            if (logger.isErrorEnabled()) {
                logger.error(format(format, arg1, arg2));
            }
        }

        @Override
        public void error(String format, Object... arguments) {
            if (logger.isErrorEnabled()) {
                logger.error(format(format, arguments));
            }
        }

        @Override
        public void error(String msg, Throwable t) {
            if (logger.isErrorEnabled()) {
                logger.error(msg, t);
            }
        }

        @Override
        public boolean isErrorEnabled(Marker marker) {
            return logger.isErrorEnabled();
        }

        @Override
        public void error(Marker marker, String msg) {
            error(msg);
        }

        @Override
        public void error(Marker marker, String format, Object arg) {
            error(format, arg);
        }

        @Override
        public void error(Marker marker, String format, Object arg1, Object arg2) {
            error(format, arg1, arg2);
        }

        @Override
        public void error(Marker marker, String format, Object... arguments) {
            error(format, arguments);
        }

        @Override
        public void error(Marker marker, String msg, Throwable t) {
            error(msg, t);
        }

        private String format(String format, Object... arguments) {
            return MessageFormatter.arrayFormat(format, arguments).getMessage();
        }

    }

}
