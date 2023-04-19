package io.gsi.hive.platform.player.mesh.gateway;

public class URLPresets {
    private URLPresets() {
    }

    public static final String MESH_N2N_URL = "http://mesh-node-igp-%s/mesh/n2n";
    public static final String OPERATOR_URL = MESH_N2N_URL + "/igp/%s";
    public static final String PLAYER_URL = OPERATOR_URL + "/player";
    public static final String AMPERSAND = "&";

    private static final String RGS_CODE_REQUEST_PARAM = "rgsCode=hive";


    public static String getValidateGuestLaunchURL(String igpCode, String authToken) {
        return String.format(OPERATOR_URL + "/guest/validateLaunch?authToken=%s", igpCode, igpCode, authToken);
    }

    public static String getPlayerUrl(String igpCode, String playerId) {
        return String.format(PLAYER_URL + "/%s?" + RGS_CODE_REQUEST_PARAM, igpCode, igpCode, playerId);
    }

    public static String getWalletUrl(String igpCode, String playerId) {
        return String.format(PLAYER_URL + "/%s/wallet?" + RGS_CODE_REQUEST_PARAM, igpCode, igpCode, playerId);
    }

    public static String getWalletUrl(String igpCode, String playerId, String rgsGameId) {
        return String.format(PLAYER_URL + "/%s/wallet?" + RGS_CODE_REQUEST_PARAM + AMPERSAND + "rgsGameId=%s",
                igpCode, igpCode, playerId, rgsGameId);
    }

    public static String processTxnUrl(String igpCode) {
        return String.format(OPERATOR_URL + "/txn?" + RGS_CODE_REQUEST_PARAM, igpCode, igpCode);
    }

    public static String cancelTxnUrl(String igpCode, String rgsTxnId) {
        return String.format(OPERATOR_URL + "/txn/%s/cancel?" + RGS_CODE_REQUEST_PARAM, igpCode, igpCode, rgsTxnId);
    }

    public static String authUrl(String igpCode, String playerId) {
        return String.format(PLAYER_URL + "/%s/auth?" + RGS_CODE_REQUEST_PARAM, igpCode, igpCode, playerId);
    }

    public static String authUrl(String igpCode, String playerId, String rgsGameId) {
        return String.format(PLAYER_URL + "/%s/auth?" + RGS_CODE_REQUEST_PARAM + AMPERSAND + "rgsGameId=%s",
                igpCode, igpCode, playerId, rgsGameId);
    }

}
