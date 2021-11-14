package com.example.iqbooster.login;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.iqbooster.R;
import com.example.iqbooster.model.User;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;

import java.util.HashMap;


public class SetUpAccountActivity extends AppCompatActivity {

    private final int mpl = 10; // padding len
    private final int mnpl = 0; // no padding len
    private ImageView mainPhoto;
    private ImageView mFirstRecommend;
    private ImageView mSecondRecommend;
    private ImageView mThirdRecommend;
    private EditText mUsername;
    private EditText mPreferName;
    private TextView mLocation;
    private MaterialButton mContinueBtn;
    private TextView mUpload;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabaseReference;

    // use to identify selected avatar
    private int mcurrentSelect;
    ActivityResultLauncher<Intent> LocationResultLauncher;

    private final String TAG = "SetupAccountActivity";
    private StorageReference firebaseStorageProfileImageRef;
    private StorageTask uploadTask;
    String profileLink = "";
    Uri profileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_up_account);

        mAuth = FirebaseAuth.getInstance();
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        firebaseStorageProfileImageRef = FirebaseStorage.getInstance().getReference().child(getResources().getString(R.string.db_profile_image));
        profileUri = Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/drawable/" +
                R.drawable.avatar);

        mcurrentSelect = 0;
        mUpload = findViewById(R.id.setup_select_custom_photo_text);
        mUsername = findViewById(R.id.setup_user_name_edit);
        mPreferName = findViewById(R.id.setup_prefer_name_edit);
        mLocation = (TextView) findViewById(R.id.setup_location_edit);
        mContinueBtn = findViewById(R.id.setup_continue_btn);
        mainPhoto = findViewById(R.id.setup_photo);
        mFirstRecommend = findViewById(R.id.setup_photo_recommend_1);
        mSecondRecommend = findViewById(R.id.setup_photo_recommend_2);
        mThirdRecommend = findViewById(R.id.setup_photo_recommend_3);

        mFirstRecommend.setPadding(mnpl,mnpl,mnpl,mnpl);
        mSecondRecommend.setPadding(mnpl,mnpl,mnpl,mnpl);
        mThirdRecommend.setPadding(mnpl,mnpl,mnpl,mnpl);

        LocationResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == 88) {
                            Intent data = result.getData();
                            if (data != null) {
                                mLocation.setText(data.getExtras().get("location").toString());
                            }
                        }
                    }
                });



        mUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImagePicker.with(SetUpAccountActivity.this)
                        .crop()	    			//Crop image(Optional), Check Customization for more option
                        .compress(1024)			//Final image size will be less than 1 MB(Optional)
                        .maxResultSize(1080, 1080)	//Final image resolution will be less than 1080 x 1080(Optional)
                        .start(10);

            }
        });

        mContinueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userinput_username = mUsername.getText().toString();
                String useriput_prefreame = mPreferName.getText().toString();
                String userinput_location = mLocation.getText().toString();
                if (!TextUtils.isEmpty(userinput_username.trim()) && !TextUtils.isEmpty(useriput_prefreame.trim())
                        && !TextUtils.isEmpty(userinput_location.trim())) {
                    FirebaseUser mUser = mAuth.getCurrentUser();
                    if (mUser != null) {
                        UserProfileChangeRequest addusername = new UserProfileChangeRequest.Builder()
                                .setDisplayName(useriput_prefreame).build();
                        mUser.updateProfile(addusername).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    goToTagPickerActivity();
                                    if (profileUri != null) {
                                        final StorageReference fileRef = firebaseStorageProfileImageRef
                                                .child(mAuth.getCurrentUser().getUid() + ".jpg");
                                        uploadTask = fileRef.putFile(profileUri);
                                        uploadTask.continueWithTask(new Continuation() {
                                            @Override
                                            public Object then(@NonNull Task task) throws Exception {
                                                if (!task.isSuccessful()) {
                                                    throw task.getException();
                                                }
                                                return fileRef.getDownloadUrl();
                                            }
                                        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Uri> task) {
                                                if (task.isSuccessful()) {
                                                    Uri downloadUrl = task.getResult();
                                                    profileLink = downloadUrl.toString();
                                                    HashMap<String, Object> userMap = new HashMap<>();
                                                    userMap.put("image", profileLink);
                                                }
                                            }
                                        }).addOnCompleteListener(new OnCompleteListener() {
                                            @Override
                                            public void onComplete(@NonNull Task task) {
                                                if (task.isSuccessful()) {
                                                    addUserInfotoDatabase(userinput_username, useriput_prefreame, userinput_location);
                                                }
                                            }
                                        });
                                    }



                                } else {
                                    String errormsg = task.getException().getMessage();
                                    Snackbar sn = Snackbar.make(findViewById(android.R.id.content),  "Error: " + errormsg, Snackbar.LENGTH_LONG);
                                    View view = sn.getView();
                                    TextView tv = (TextView) view.findViewById(com.google.android.material.R.id.snackbar_text);
                                    tv.setTextColor(Color.parseColor("#FFD700"));
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                                        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                    } else {
                                        tv.setGravity(Gravity.CENTER_HORIZONTAL);
                                    }
                                    sn.show();
                                }
                            }
                        });
                    }
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 10) {
            assert data != null;
            profileUri = data.getData();
            mainPhoto.setImageURI(profileUri);
        }

    }

    @Override
    public void onBackPressed() {
        // NO GO BACK
    }

    /**
     * Helper method which use Intent to go to the Bubble Picker
     */
    private void goToTagPickerActivity() {
        Intent goToDiscoverActivity = new Intent(this, DiscoverActivity.class);
        startActivity(goToDiscoverActivity);
        finish();
    }

    private void addUserInfotoDatabase(String input_username, String input_prefer_name, String input_location) {
        String useremail = mAuth.getCurrentUser().getEmail();
        String uid = mAuth.getCurrentUser().getUid();

        User currUser = new User(input_username,
                input_prefer_name,
                useremail,
                uid,
                input_location,
                "",
                "",
                "",
                profileLink);
        mDatabaseReference.child(getApplicationContext().getString(R.string.db_users))
                .child(uid)
                .setValue(currUser);
    }

    public void changeMainPhoto(View view) {
        String value = view.getTag().toString();
        if (value.equals("rec1")) {
            if (mcurrentSelect == 1) {
                mFirstRecommend.setPadding(mnpl, mnpl, mnpl, mnpl);
                mFirstRecommend.clearColorFilter();
                mainPhoto.setImageResource(R.drawable.avatar);
                profileUri = Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/drawable/" +
                        R.drawable.avatar);
                mcurrentSelect = 0;
            } else {
                mFirstRecommend.setPadding(mpl, mpl, mpl, mpl);
                mSecondRecommend.setPadding(mnpl,mnpl,mnpl,mnpl);
                mThirdRecommend.setPadding(mnpl,mnpl,mnpl,mnpl);
                mainPhoto.setImageResource(R.drawable.food);
                profileUri = Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/drawable/" +
                        R.drawable.food);
                mcurrentSelect = 1;
            }
        } else if (value.equals("rec2")) {
            if (mcurrentSelect == 2) {
                mSecondRecommend.setPadding(mnpl,mnpl,mnpl,mnpl);
                mSecondRecommend.clearColorFilter();
                mainPhoto.setImageResource(R.drawable.avatar);
                profileUri = Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/drawable/" +
                        R.drawable.avatar);
                mcurrentSelect = 0;
            } else {
                mFirstRecommend.setPadding(mnpl, mnpl, mnpl, mnpl);
                mSecondRecommend.setPadding(mpl,mpl,mpl,mpl);
                mThirdRecommend.setPadding(mnpl,mnpl,mnpl,mnpl);
                mainPhoto.setImageResource(R.drawable.sport);
                profileUri = Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/drawable/" +
                        R.drawable.sport);
                mcurrentSelect = 2;
            }
        } else {
            if (mcurrentSelect == 3) {
                mThirdRecommend.setPadding(mnpl,mnpl,mnpl,mnpl);
                mainPhoto.setImageResource(R.drawable.avatar);
                profileUri = Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/drawable/" +
                        R.drawable.avatar);
                mcurrentSelect = 0;
            } else {
                mFirstRecommend.setPadding(mnpl, mnpl, mnpl, mnpl);
                mSecondRecommend.setPadding(mnpl,mnpl,mnpl,mnpl);
                mThirdRecommend.setPadding(mpl,mpl,mpl,mpl);
                mainPhoto.setImageResource(R.drawable.entertainment);
                profileUri = Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/drawable/" +
                        R.drawable.entertainment);
                mcurrentSelect = 3;
            }
        }
    }

    public void getLocation(View view) {
        Intent goToMaps = new Intent(this, MapsActivity.class);
        LocationResultLauncher.launch(goToMaps);
    }
}