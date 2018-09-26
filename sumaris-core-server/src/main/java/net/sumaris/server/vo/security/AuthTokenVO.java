package net.sumaris.server.vo.security;

import lombok.Data;

@Data
public class AuthTokenVO extends AuthDataVO {

    public AuthTokenVO() {
        super();
    }

    public AuthTokenVO(String pubkey, String challenge, String signature) {
        super(pubkey, challenge, signature);
    }

}
