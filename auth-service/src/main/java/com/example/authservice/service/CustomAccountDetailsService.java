package com.example.authservice.service;

import com.example.authservice.auth.CustomAccountDetails;
import com.example.authservice.model.Account;
import com.example.authservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomAccountDetailsService implements UserDetailsService {
    private final AccountRepository accountRepository;
    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));

        return new CustomAccountDetails(account);
    }
}
