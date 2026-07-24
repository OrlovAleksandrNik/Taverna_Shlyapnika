package by.taverna.shlyapnika.control.security;

import by.taverna.shlyapnika.control.auth.domain.UserAccount;
import by.taverna.shlyapnika.control.auth.domain.UserStatus;
import by.taverna.shlyapnika.control.auth.infrastructure.UserAccountRepository;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ControlUserDetailsService implements UserDetailsService {
  private final UserAccountRepository users;

  public ControlUserDetailsService(UserAccountRepository users) {
    this.users = users;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    UserAccount user = users.findByEmail(username.toLowerCase())
        .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    return new Principal(user);
  }

  public record Principal(UserAccount user) implements UserDetails {
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
      return RoleCatalog.permissionsFor(user.getRoles()).stream()
          .map(permission -> new SimpleGrantedAuthority(permission.value()))
          .toList();
    }

    @Override
    public String getPassword() { return user.getPasswordHash(); }

    @Override
    public String getUsername() { return user.getEmail(); }

    @Override
    public boolean isAccountNonLocked() { return user.getStatus() != UserStatus.BLOCKED; }

    @Override
    public boolean isAccountNonExpired() { return user.getStatus() != UserStatus.DELETED; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return user.getStatus() == UserStatus.ACTIVE || user.getStatus() == UserStatus.INVITED; }
  }
}
