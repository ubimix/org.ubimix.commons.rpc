/**
 * 
 */
package org.webreformatter.commons.rpc;

/**
 * @author kotelnikov
 */
public class DefaultRpcMethodProvider implements IRpcMethodProvider {

    /**
     * 
     */
    public DefaultRpcMethodProvider() {
    }

    /**
     * @see org.webreformatter.commons.rpc.IRpcMethodProvider#getMethodName(java.lang.Class)
     */
    public String getMethodName(Class<? extends RpcEvent> type) {
        String name = type.getName();
        int idx = name.lastIndexOf(".");
        if (idx > 0) {
            name = name.substring(idx + 1);
        }
        StringBuilder buf = new StringBuilder();
        boolean begin = true;
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (ch == '$') {
                buf.append('.');
                begin = true;
            } else {
                if (begin) {
                    ch = Character.toLowerCase(ch);
                }
                begin = false;
                buf.append(ch);
            }
        }
        return buf.toString();
    }

}
