package com.eft.pdfpicker;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.dropbox.chooser.android.DbxChooser;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.OpenFileActivityBuilder;
import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    protected static final int REQUEST_CODE_RESOLUTION = 1;
    private static final int DBX_CHOOSER_REQUEST = 122;
    private static final int DIR_SELECT_REQ_CODE = 98;
    private static final int REQUEST_CODE_OPENER = 44;
    private static final String TAG = "MainActivity";
    TextView path;
    private DbxChooser mChooser;
    private GoogleApiClient mGoogleApiClient;
    private ImageView contentImage;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        path = (TextView) findViewById(R.id.path);
        contentImage = (ImageView) findViewById(R.id.contentImage);
        mChooser = new DbxChooser("byumqcjim5vaira");
    }

    /**
     * Called when activity gets visible. A connection to Drive services need to
     * be initiated as soon as the activity is visible. Registers
     * {@code ConnectionCallbacks} and {@code OnConnectionFailedListener} on the
     * activities itself.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addScope(Drive.SCOPE_APPFOLDER) // required for App Folder sample
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();

    }
    /**
     * Local den pdf seçimi
     *
     * */
    public void onPdfSelectClick(View view) {
        new MaterialFilePicker()
                .withActivity(this)
                .withRequestCode(DIR_SELECT_REQ_CODE)
//                .withFilter(Pattern.compile(".*\\.pdf$")) // Filtering files and directories by file name using regexp
                .withFilterDirectories(true) // Set directories filterable (false by default)
                .withHiddenFiles(false) // Show hidden files and folders
                .start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == DIR_SELECT_REQ_CODE && resultCode == RESULT_OK) {
            String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            open_File(filePath);
            // Do anything with file
            path.setText(filePath);
        }
        if (requestCode == DBX_CHOOSER_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                DbxChooser.Result result = new DbxChooser.Result(data);
                File myCopiedPdf = new File(getCacheDir() +"/" +result.getName());
                File dropBoxPdf = new File(result.getLink().getPath());
                copy(dropBoxPdf,myCopiedPdf);
                Picasso.with(MainActivity.this).load(result.getLink().toString()).into(contentImage);
                open_File(myCopiedPdf.getPath());
                path.setText(result.getLink().toString());

                // Handle the result
            } else {
                // Failed or was cancelled by the user.
            }
        }
        if (requestCode == REQUEST_CODE_RESOLUTION && resultCode == RESULT_OK) {
            mGoogleApiClient.connect();
        }
        if (requestCode == REQUEST_CODE_OPENER && resultCode == RESULT_OK) {
            final DriveId driveId = (DriveId) data.getParcelableExtra(
                    OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
            driveId.asDriveFile().getMetadata(mGoogleApiClient).setResultCallback(new ResultCallback<DriveResource.MetadataResult>() {
                @Override
                public void onResult(@NonNull DriveResource.MetadataResult metadataResult) {
                    pullFileWithDriveId(driveId,metadataResult.getMetadata().getTitle());
                }
            });





            path.setText("Selected file's ID: " + driveId);
        }
    }
    public void copy(File src, File dst) {
        try {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dst);

            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static byte[] fullyReadFileToBytes(File f) throws IOException {
        int size = (int) f.length();
        byte bytes[] = new byte[size];
        byte tmpBuff[] = new byte[size];
        FileInputStream fis= new FileInputStream(f);;
        try {

            int read = fis.read(bytes, 0, size);
            if (read < size) {
                int remain = size - read;
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain);
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                    remain -= read;
                }
            }
        }  catch (IOException e){
            throw e;
        } finally {
            fis.close();
        }

        return bytes;
    }
    private static String encodeFileToBase64Binary(File fileName) throws IOException {
        byte[] bytes = fullyReadFileToBytes(fileName);
        String encodedString = Base64.encodeToString(bytes,Base64.DEFAULT);
        return encodedString;
    }
    private void pullFileWithDriveId(DriveId driveId, final String filename) {
        driveId.asDriveFile().open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, new DriveFile.DownloadProgressListener() {
            @Override
            public void onProgress(long l, long l1) {
                Log.d("DownloadProgress", "l:   " + l +"    l1 :    "+l1);
            }
        })
                .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
                    @Override
                    public void onResult(@NonNull DriveApi.DriveContentsResult result) {
                        if (!result.getStatus().isSuccess()) {
                            // display an error saying file can't be opened
                            return;
                        }
                        // DriveContents object contains pointers
                        // to the actual byte stream
                        DriveContents contents = result.getDriveContents();
                        File file = new File(getCacheDir(), filename);
                        copyInputStreamToFile(contents.getInputStream(),file);
                        open_File(file.getPath());
                        contents.discard(mGoogleApiClient);
                    }
                });
    }

    private void copyInputStreamToFile(InputStream in, File file ) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while((len=in.read(buf))>0){
                out.write(buf,0,len);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void open_File(String filePath){
        final File file = new File(filePath);
        int file_size = Integer.parseInt(String.valueOf(file.length()/1024));
        Log.d(TAG, "file_size:" + file_size);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    long currentTime = System.currentTimeMillis();
                    String s =  encodeFileToBase64Binary(file);
                    long finishTime = System.currentTimeMillis();
                    Log.d(TAG, "time" + (finishTime - currentTime));
                    if (s != null) {
//                        Log.d(TAG, "s got");
                        System.gc();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
       /* Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), "application/pdf");
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        Intent intent1 = Intent.createChooser(intent, "Open With");
        try {
            startActivity(intent1);
        } catch (ActivityNotFoundException e) {
            // Instruct the user to install a PDF reader here, or something
        }*/
    }
    /**
     * Dropbox File seçimi
     * */
    public void onDropboxSelectClick(View view) {
        mChooser.forResultType(DbxChooser.ResultType.FILE_CONTENT)
                .launch(MainActivity.this, DBX_CHOOSER_REQUEST);
    }
    /**
     * Google drive File seçimi
     * */
    public void onDriveSelectClick(View view) {
        IntentSender intentSender = Drive.DriveApi
                .newOpenFileActivityBuilder()
                .setMimeType(new String[] { "application/pdf" })
                .build(getGoogleApiClient());
        try {
            startIntentSenderForResult(
                    intentSender, REQUEST_CODE_OPENER, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            Log.w(TAG, "Unable to send intent", e);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }


    /**
     * Called when {@code mGoogleApiClient} is trying to connect but failed.
     * Handle {@code result.getResolution()} if there is a resolution is
     * available.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(this, result.getErrorCode(), 0).show();
            return;
        }
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
    }

    /**
     * Getter for the {@code GoogleApiClient}.
     */
    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }
}
