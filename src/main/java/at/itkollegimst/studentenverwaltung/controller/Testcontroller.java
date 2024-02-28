package at.itkollegimst.studentenverwaltung.controller;

import at.itkollegimst.studentenverwaltung.exceptions.AuthUserNotFoundInDbException;
import at.itkollegimst.studentenverwaltung.exceptions.NoAuthHeaderFoundException;
import at.itkollegimst.studentenverwaltung.jwt.JwtTokenService;
import at.itkollegimst.studentenverwaltung.jwt.JwtUserDetails;
import at.itkollegimst.studentenverwaltung.jwt.JwtUserDetailsService;
import at.itkollegimst.studentenverwaltung.request.AuthenticationRequest;
import at.itkollegimst.studentenverwaltung.request.AuthenticationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.token.TokenService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.Principal;

@RestController
public class Testcontroller
{
    JwtTokenService jwtTokenService;
    JwtUserDetailsService jwtUserDetailsService;

    AuthenticationManager authenticationManager;

    public Testcontroller(JwtTokenService jwtTokenService, JwtUserDetailsService jwtUserDetailsService, AuthenticationManager authenticationManager) {
        this.jwtTokenService = jwtTokenService;
        this.jwtUserDetailsService = jwtUserDetailsService;
        this.authenticationManager = authenticationManager;
    }

    @GetMapping("/test")
    public String test()
    {
        return "TEST";
    }

    @GetMapping("/secure")
    public String secure(Principal p)
    {
        return "Hallo " + p.getName() + "! Dies ist die Antwort eines REST-Endpunkts der nur für eingeloggte Benutzer erreichbar ist.";
    }

    @GetMapping("/secureforroleuser")
    public String secureForRoleUser(Principal p)
    {
        return "Hallo " +  p.getName() + "! Dies ist die Antwort eines REST-Endpunktes der nur für eingloggte Benutzer mit der Rolle ROLE_USER erreichbar ist!";
    }

    @GetMapping("/secureforroleadmin")
    public String secureForRoleAdmin(Principal p)
    {
        return "Hallo " +  p.getName() + "! Dies ist die Antwort eines REST-Entdpunktes der nur für eingloggte Benutzer mit der Rolle ROLE_ADMIN erreichbar ist!";
    }

    @PostMapping("/authenticate")
    public AuthenticationResponse authenticate(@RequestBody @Valid final AuthenticationRequest authenticationRequest) {
        try{
            System.out.println(authenticationRequest.getLogin());
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    authenticationRequest.getLogin(), authenticationRequest.getPassword()));
        } catch (final BadCredentialsException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        final UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(authenticationRequest.getLogin());
        final AuthenticationResponse authenticationResponse = new AuthenticationResponse();
        authenticationResponse.setAccessToken(jwtTokenService.generateAccessToken(userDetails));
        authenticationResponse.setRefreshToken(jwtTokenService.generateRefreshToken(userDetails));
        return authenticationResponse;
    }

    @PostMapping("/refreshtoken")//Route abgesichert, nur mit gültigem Token aufrufbar
    public void refreshToken(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        //Token auslesen --> muss gültig sein, sonst wäre diese Route /refreshtoken durch Spring-Security abgesichert und nicht erreichbar
        final String header = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            throw new NoAuthHeaderFoundException();
        }
        final String refreshToken = header.substring(7);
        final String username = jwtTokenService.validateTokenAndGetUsername(refreshToken);
        if (username != null) {
            final JwtUserDetails userDetails = jwtUserDetailsService.loadUserByUsername(username);
            //neuen Access-Token für Benutzer generieren
            var accessToken = jwtTokenService.generateAccessToken(userDetails);
            //neuen Response zusammenbauen
            var authResponse = new AuthenticationResponse();
            authResponse.setRefreshToken(refreshToken);//bleibt der gleiche
            authResponse.setAccessToken(accessToken);//neuer Access Token
            //Response-Daten in den HTTP-Response-Body schreiben
            new ObjectMapper().writeValue(httpServletResponse.getOutputStream(),authResponse);//Response umschreiben
        } else
        {
            throw new AuthUserNotFoundInDbException();
        }
    }

    //Todo: man müsste noch einen Token-Store (nicht nur in Memory so wie jetzt) einbauen, z.B. Redis. Das würde besser Skalieren und man könnte Tokens dann auch entziehen.


    /*
    Anwendung:
    Post-Request auf localhost:8080/authenticate mit folgenden JSON-Body:
    {
	"login":"Claudio",
	"password":"MeinPasswort"
    }

    Get-Request auf localhost:8080/secure mit Bearer-Authentication und Token mitsenden

     */
}
