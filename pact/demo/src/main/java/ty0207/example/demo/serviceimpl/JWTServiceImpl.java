package ty0207.example.demo.serviceimpl;

import io.jsonwebtoken.*;
import io.jsonwebtoken.SignatureAlgorithm;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import net.sf.json.JSONObject;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ty0207.example.demo.service.JWTService;

@Service
public class JWTServiceImpl implements JWTService {
    /* token 过期时间, 单位: 秒. 这个值表示 30 天 */

    /**
     * 创建JWT
     */
    private String createJWT(Map<String, Object> claims, Long time, String key) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RS256;
        Date now = new Date(System.currentTimeMillis());

        PrivateKey  secretKey = loadPrivateKey();
        long nowMillis = System.currentTimeMillis();//生成JWT的时间
        //下面就是在为payload添加各种标准声明和私有声明了
        JwtBuilder builder = Jwts.builder() //这里其实就是new一个JwtBuilder，设置jwt的body
                .setClaims(claims)          //如果有私有声明，一定要先设置这个自己创建的私有的声明，这个是给builder的claim赋值，一旦写在标准的声明赋值之后，就是覆盖了那些标准的声明的
                .setIssuedAt(now)        //iat: jwt的签发时间
                .signWith(signatureAlgorithm, secretKey)
                .setIssuer("casecloud.com.cn");
        if (time >= 0) {
            long expMillis = nowMillis + time;
            Date exp = new Date(expMillis);
            System.out.println(exp);
            builder.setExpiration(exp);     //设置过期时间
        }
        return builder.compact();
    }

    /**
     * 验证jwt
     */
    public Claims verifyJwt(String token, String keyString) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        //签名秘钥，和生成的签名的秘钥一模一样
        Key key = loadPublicKey();
        Claims claims;
        try {
            claims = Jwts.parser()  //得到DefaultJwtParser
                    .setSigningKey(key)         //设置签名的秘钥
                    .parseClaimsJws(token).getBody();
        } catch (Exception e) {
            claims = null;
        }//设置需要解析的jwt
        return claims;

    }

    private PublicKey loadPublicKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        ClassPathResource key = new ClassPathResource("/static/rsa_public_key_2048.pub");
        InputStream in = key.getInputStream();
        PemObject pem = new PemReader(new InputStreamReader(in)).readPemObject();
        byte[] pubKeyBytes = pem.getContent();
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(pubKeyBytes);
        return keyFactory.generatePublic(pubKeySpec);
    }


    /**
     * 加载private key
     *
     * @return
     */
    private PrivateKey loadPrivateKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        ClassPathResource key = new ClassPathResource("/static/rsa_private_key_pkcs8.pem");
        InputStream in = key.getInputStream();
        PemObject pem = new PemReader(new InputStreamReader(in)).readPemObject();
        byte[] privateKeyBytes = pem.getContent();
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        return keyFactory.generatePrivate(privKeySpec);
    }

    /**
     * 根据userId和openid生成token
     */
    public ResponseEntity<?> generateToken(Integer userId, ArrayList<String> scopes, String key) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("scope", scopes);
        Long TOKEN_EXPIRED_TIME = 1000 * 24 * 60 * 60L;
        JSONObject result = new JSONObject();
        result.put("token", createJWT(map, TOKEN_EXPIRED_TIME, key));
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
