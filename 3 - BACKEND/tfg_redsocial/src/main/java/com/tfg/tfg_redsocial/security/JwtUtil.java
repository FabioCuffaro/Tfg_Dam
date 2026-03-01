package com.tfg.tfg_redsocial.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

/**
 * JwtUtil - Caja de herramientas para trabajar con tokens JWT
 *
 * Esta clase hace tres cosas:
 *   1. generateToken()   → crea un nuevo token cuando el usuario hace login
 *   2. validateToken()   → comprueba que un token es auténtico y no ha expirado
 *   3. extractEmail()    → lee el email del usuario guardado dentro del token
 *
 * @Component → Spring gestiona esta clase automáticamente.
 * La podemos inyectar en otras clases sin instanciarla con "new" directamente.
 */
@Component
public class JwtUtil {

    /**
     * Clave secreta para firmar los tokens.
     * Spring lee este valor directamente de application.properties (jwt.secret).
     * Ahora mismo es una clave que he creado yo, pero luego cuando se despliegue tendre que cambiarlo y poner
     * una variable de entorno, pero para hacer pruebas funciona ahora mismo
     */
    private final String secret;

    /**
     * Tiempo de vida del token en milisegundos.
     * Spring lee este valor de application.properties (jwt.expiration).
     * 86400000 ms = 24 horas, como he definido en los requisitos no funcionales.
     */
    private final Long expiration;

    // Inyección por constructor en lugar de @Value en campo
    // Esto garantiza que los valores están disponibles
    // en el momento en que Spring crea el bean
    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") Long expiration) {
        this.secret = secret;
        this.expiration = expiration;
    }



    //==============================================
    // MÉTODO 1: Generar token
    //==============================================

    /**
     * Genera un token JWT para cuando una persona se loguee en la aplicación
     *
     * El token contiene:
     *   - subject: el email del usuario (lo usamos como identificador)
     *   - issuedAt: fecha/hora de creación
     *   - expiration: fecha/hora de expiración (ahora + 24h)
     *   - firma: garantiza que nadie ha manipulado el contenido
     *
     *  Lo cojo directamente de una clase que es Jwts que junto a builder para crear el token en sí
     *
     * @param email El email del usuario autenticado
     * @return El token JWT como String (ej: "eyJhbGci...")
     */
    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)                          // quién es el usuario
                .setIssuedAt(new Date())                    // cuándo se ha creado
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // cuándo expira Y LO TENGO QUE PONER EN MILISEGUNDOS
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // firma con nuestra clave
                .compact();                                 // construye el String final
    }



    /**
     * Metodo privado necesario para recoger la llave secreta
     *
     * Convierte el String secreto de application.properties en una Key criptográfica.
     *
     * JWT necesita un objeto Key, no un String simple.
     * Keys.hmacShaKeyFor() convierte los bytes del secreto en la clave correcta.
     *
     *
     * Es una de las partes más complejas, ya que hay un
     * algoritmo que he usado arriba, el signature.
     * Este, HS256 requiere mínimo 256 bits (32 bytes).
     * Entonces pasa la clave a UTF-8 y lo hago en un
     * array que vendrá con la longitud al pasar la clave
     * a Bytes-UTF8 variables
     */
    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes(); //array de bytes
        return Keys.hmacShaKeyFor(keyBytes);
    }




    //==============================================
    // MÉTODO 2: Validar token
    //==============================================

    /**
     * Me dice si el token que utiliza ese perfil está expirado o no
     *
     * Comprueba dos cosas:
     *   1. Que el email dentro del token coincide con el email del usuario
     *   2. Que el token no haya expirado
     *
     * Lo bueno es que si alguien me hackea y el token no es el mismo, lanza excepciones automáticamente
     *
     * @param token El token JWT que llegó en la petición HTTP
     * @param email El email del usuario que estamos verificando
     * @return true si el token es válido, false si no
     */
    public boolean validateToken(String token, String email) {
        //Variable que será una constante para cada email claro, tengo que crear un método que pase el token a email
        final String tokenEmail = extractEmail(token);
        return (tokenEmail.equals(email) && !isTokenExpired(token));
    }



    /**
     * Como he visto que tengo que hacer varios métodos para pasar de tokens a emails
     * he creado varios métodos privados a los que no se tendrá acceso fuera de esta clase.
     *
     * Método genérico para extraer cualquier dato (claim) del token.
     *
     * Claims son los datos guardados dentro del JWT.
     * Usamos una Function para poder extraer cualquier campo
     * sin repetir código (subject, expiration, etc.)
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }


    /**
     * Para hacer lo que tengo arriba, también necesito saber toooodos los datos del JWT
     *
     * Parseo el token completo y devuelvo todos sus datos internos (Claims).
     *
     * Aquí es donde JJWT va a verificar la firma. Entonces... si alguien ha cambiado esto
     * lanza una excepción y la petición se rechaza.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey()) // usamos la misma clave con la que firmamos
                .build()
                .parseClaimsJws(token)
                .getBody();
    }



    //==============================================
    // MÉTODO 3: Extraer información del token
    //==============================================

    /**
     * Extrae el email (subject) del token.
     * El email fue guardado dentro del token al generarlo con setSubject().
     *
     * @param token El token JWT
     * @return El email del usuario guardado en el token
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }




    //==============================================
    // MÉTODOS NECESARIOS PARA COMPROBAR COSAS DEL TOKEN Y EMAIL
    //==============================================


    /**
     * Comprueba si el token ha expirado comparando su fecha de expiración con ahora.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extrae la fecha de expiración del token.
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }



}