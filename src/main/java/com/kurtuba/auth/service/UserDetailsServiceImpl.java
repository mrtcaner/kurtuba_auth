package com.kurtuba.auth.service;

import com.kurtuba.auth.data.model.User;
import com.kurtuba.auth.data.model.dto.KurtubaUserDetailsDto;
import com.kurtuba.auth.data.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UserDetailsServiceImpl implements UserDetailsService {

	private final UserRepository userRepository;

	public UserDetailsServiceImpl(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String usernameEmail)
			throws UsernameNotFoundException {
		User user = userRepository.getUserByEmailOrUsername(usernameEmail);
		
		if (user == null) {
			throw new UsernameNotFoundException("Could not find user");
		}

		List<SimpleGrantedAuthority> auths = new ArrayList<>();
		user.getUserRoles().stream().map(auth -> auths.add(new SimpleGrantedAuthority(auth.getRole().name())));
		return KurtubaUserDetailsDto.builder()
				.username(user.getUsername())
				.password(user.getPassword())
				.authorities(auths)
				.locked(user.isLocked())
				.activated(user.isActivated())
				.build();
	}

}
