package com.nimbus.backend.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

@Component
@Slf4j
public class DnsVerificationUtil {

    private static final String SYSTEM_CNAME_TARGET = "cname.nimbus.app";

    public boolean verifyCnameRecord(String customDomain) {
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            InitialDirContext dirContext = new InitialDirContext(env);

            Attributes attrs = dirContext.getAttributes(customDomain, new String[]{"CNAME"});
            if (attrs.get("CNAME") != null) {
                String cnameValue = attrs.get("CNAME").get().toString();
                // Strip trailing dot if present in DNS response
                if (cnameValue.endsWith(".")) {
                    cnameValue = cnameValue.substring(0, cnameValue.length() - 1);
                }
                log.info("DNS CNAME lookup for {}: resolved to {}", customDomain, cnameValue);
                return SYSTEM_CNAME_TARGET.equalsIgnoreCase(cnameValue);
            }
        } catch (Exception e) {
            log.warn("Failed CNAME lookup for domain: {}", customDomain, e);
        }
        return false;
    }
}