package org.springframework.boot;

/**
 * Compatibility shim for libraries compiled against older Spring Boot package layout.
 */
public class ConfigurableBootstrapContext extends org.springframework.boot.bootstrap.DefaultBootstrapContext {
    public ConfigurableBootstrapContext() {
        super();
    }
}
