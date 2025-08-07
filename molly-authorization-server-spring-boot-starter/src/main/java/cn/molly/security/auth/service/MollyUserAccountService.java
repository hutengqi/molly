package cn.molly.security.auth.service;

import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * <p>
 * Extends {@link UserDetailsService} to provide a unified user account service
 * for the Molly security framework. This interface is intended to be the central
 * point for loading user data, supporting various authentication methods in the future,
 * such as phone number or social media logins.
 * </p>
 * <p>
 * Implementations of this interface are responsible for retrieving user details
 * from the underlying data store (e.g., database, LDAP, etc.).
 * </p>
 *
 * @author Ht7_Sincerity
 * @since 2025/8/7
 */
public interface MollyUserAccountService extends UserDetailsService {
}
