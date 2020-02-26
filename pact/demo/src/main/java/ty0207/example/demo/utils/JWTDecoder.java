package ty0207.example.demo.utils;

import java.util.Base64;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import net.sf.json.JSONObject;

public class JWTDecoder {

    /**
     * @param jwtString is a dot-separated, three-part standard jwt String.
     */

    public static JSONObject decodeJWT (String jwtString) {
        try {
            String[] separations = jwtString.split("\\.");
            String middlePart = separations[1];
            String decoded = new String(Base64.getDecoder().decode(middlePart), "UTF-8");
            JSONObject result = JSONObject.fromObject(decoded);
            return result;
        }
        catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static JSONObject parseJWTRequest (HttpServletRequest request) {
        JSONObject userInfo = null;
        Cookie[] userCk = request.getCookies();
        String jwtStr = null;
        for (int i = 0; i < userCk.length; i++) {
            if (userCk[i].getName().equals("key")) {
                jwtStr = userCk[i].getValue();
                try {
                    userInfo = JWTDecoder.decodeJWT(jwtStr);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    JSONObject errorInfo = new JSONObject();
                    errorInfo.accumulate("info", "error in JWTDecoder: " + e.getMessage());
                    errorInfo.accumulate("stateCode", 500);
                    errorInfo.accumulate("succeeded", false);
                    return errorInfo;
                }
                System.out.println(userInfo);
                break;
            }
            if (i == userCk.length - 1) {
                JSONObject errorInfo = new JSONObject();
                errorInfo.accumulate("info", "jwt not found in cookie" );
                errorInfo.accumulate("stateCode", 403);
                errorInfo.accumulate("succeeded", false);
                return errorInfo;
            }
        }
        if (userInfo == null) {
            JSONObject errorInfo = new JSONObject();
            errorInfo.accumulate("info", "jwtKey parse error, witch is : " + jwtStr);
            errorInfo.accumulate("stateCode", 501);
            errorInfo.accumulate("succeeded", false);
            return errorInfo;
        }
        userInfo.accumulate("stateCode", 200);
        return userInfo;
    }
}
