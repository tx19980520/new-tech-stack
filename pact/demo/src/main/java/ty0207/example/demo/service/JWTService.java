package ty0207.example.demo.service;

import io.jsonwebtoken.Claims;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;

public interface JWTService {
    public Claims verifyJwt(String token, String keyString) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException;
    public ResponseEntity<?> generateToken(Integer userId, ArrayList<String> scopes, String key) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException;
}
