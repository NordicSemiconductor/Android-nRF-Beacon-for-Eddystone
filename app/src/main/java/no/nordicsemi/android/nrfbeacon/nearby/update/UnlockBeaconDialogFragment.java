package no.nordicsemi.android.nrfbeacon.nearby.update;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import no.nordicsemi.android.nrfbeacon.nearby.R;
import no.nordicsemi.android.nrfbeacon.nearby.util.ParserUtils;

/**
 * Created by rora on 07.03.2016.
 */
public class UnlockBeaconDialogFragment extends DialogFragment {

    private static final String PATTERN_TX_POWER = "[0-9a-fA-F]{32}";
    private static final String CHALLENGE = "CHALLENGE";
    private static final String UNLOCK_MESSAGE = "UNLOCK_MESSAGE";
    private static final String TAG = "BEACON";
    private EditText mUnlockCode;
    private OnBeaconUnlockListener beaconUnlockListener;
    private byte [] mChallenge;
    private String mUnlockMessage;

    public interface OnBeaconUnlockListener {
        void unlockBeacon(byte [] encryptedLockCode, final byte [] beaconLockCode);
        void cancelUnlockBeacon();
    }

    public static UnlockBeaconDialogFragment newInstance(byte [] challenge, final String message){
        UnlockBeaconDialogFragment fragment = new UnlockBeaconDialogFragment();
        final Bundle args = new Bundle();
        args.putByteArray(CHALLENGE, challenge);
        args.putString(UNLOCK_MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }

    public UnlockBeaconDialogFragment(){

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            mChallenge = getArguments().getByteArray(CHALLENGE);
            mUnlockMessage = getArguments().getString(UNLOCK_MESSAGE);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(getString(R.string.unlock_beacon_title));
        alertDialogBuilder.setMessage(mUnlockMessage);
        final View alertDialogView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_dialog_unlock, null);
        mUnlockCode = (EditText) alertDialogView.findViewById(R.id.lock_code);
        mUnlockCode.setText("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");

        final AlertDialog alertDialog = alertDialogBuilder.setView(alertDialogView).setPositiveButton(getString(R.string.unlock), null).setNegativeButton(getString(R.string.cancel), null).show();
        alertDialog.setCanceledOnTouchOutside(false);

        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(validateInput()) {
                    final String lockCode = mUnlockCode.getText().toString().trim();
                    byte [] beaconLockCode = new byte[16];
                    ParserUtils.setByteArrayValue(beaconLockCode, 0, lockCode);
                    byte [] encryptedLockCode = null;
                    if(mChallenge != null)
                        encryptedLockCode = ParserUtils.aes128Encrypt(mChallenge, new SecretKeySpec(beaconLockCode, "AES"));
                    dismiss();
                    ((OnBeaconUnlockListener)getParentFragment()).unlockBeacon(encryptedLockCode, beaconLockCode);
                }
            }
        });

        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                ((OnBeaconUnlockListener)getParentFragment()).cancelUnlockBeacon();
            }
        });

        return alertDialog;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private boolean validateInput(){
        final String lockCode = mUnlockCode.getText().toString().trim();
        if(lockCode.isEmpty()){
            mUnlockCode.setError("Please enter the lock code to unlock the beacon");
            return false;
        } else if (!lockCode.matches(PATTERN_TX_POWER)) {
            mUnlockCode.setError("Please enter a valid value for new lock code");
            return false;
        }
        return true;
    }

    private byte[] aes128Encrypt(byte[] data, SecretKeySpec keySpec) {
        Cipher cipher;
        try {
            // Ignore the "ECB encryption should not be used" warning. We use exactly one block so
            // the difference between ECB and CBC is just an IV or not. In addition our blocks are
            // always different since they have a monotonic timestamp. Most importantly, our blocks
            // aren't sensitive. Decrypting them means means knowing the beacon time and its rotation
            // period. If due to ECB an attacker could find out that the beacon broadcast the same
            // block a second time, all it could infer is that for some reason the clock of the beacon
            // reset, which is not very helpful
            cipher = Cipher.getInstance("AES/ECB/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            Log.e(TAG, "Error constructing cipher instance", e);
            return null;
        }

        try {
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        } catch (InvalidKeyException e) {
            Log.e(TAG, "Error initializing cipher instance", e);
            return null;
        }

        byte[] ret;
        try {
            ret = cipher.doFinal(data);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            Log.e(TAG, "Error executing cipher", e);
            return null;
        }

        return ret;
    }

}
