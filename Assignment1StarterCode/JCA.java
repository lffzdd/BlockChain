import java.security.*;
import java.util.Arrays;

public class JCA {
    public static void main(String[] args) throws Exception {
        String message = "Hello, World!";
        byte[] messageBytes = message.getBytes();


        // 数字签名,签名需要使用私钥, 验证需要使用公钥,所以需要先生成密钥对
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048); // 初始化密钥对生成器, 指定密钥长度为2048位
        KeyPair keyPair = keyPairGenerator.generateKeyPair(); // 生成密钥对

        PrivateKey privateKey = keyPair.getPrivate(); // 获取私钥
        PublicKey publicKey = keyPair.getPublic(); // 获取公钥
        System.out.println("私钥: " + Arrays.toString(privateKey.getEncoded()));
        System.out.println("公钥: " + publicKey.getEncoded());

        /*
         * 数字签名步骤如下 
         * 1. 初始化签名对象:initSign(PrivateKey key) 
         * 2. 更新需要签名的数据:update(byte[] data) 
         * 3. 生成签名:sign() 
         * 4. 初始化验证对象:initVerify(PublicKey key) 
         * 5. 更新需要验证的数据:update(byte[] data) 
         * 6. 验证签名:verify(byte[] signature)
         */

        Signature sig = Signature.getInstance("SHA256withRSA");
        // 签名
        sig.initSign(privateKey);
        sig.update(messageBytes);
        byte[] digitalSignature = sig.sign(); // sign()方法返回签名结果,会把update()方法中传入的数据进行签名

        // 初始化验证对象
        sig.initVerify(publicKey); // 初始化验证对象, 使用公钥
        sig.update(messageBytes);
        System.out.println("验证签名: " + sig.verify(digitalSignature));
    }
}
