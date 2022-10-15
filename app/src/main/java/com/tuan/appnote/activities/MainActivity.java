package com.tuan.appnote.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.tuan.appnote.R;
import com.tuan.appnote.adapters.NotesAdapter;
import com.tuan.appnote.databases.NotesDatabase;
import com.tuan.appnote.entities.Note;
import com.tuan.appnote.listeners.NotesListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NotesListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        addControlls();
        addEvents();
        getNotes(REQUEST_CODE_SHOW_NOTE, false);
    }
    public static final int REQUEST_CODE_SHOW_NOTE = 3;
    public static final int REQUEST_CODE_ADD_NOTE = 1;
    public static final int REQUEST_CODE_UPDATE_NOTE = 2;
    public static final int REQUEST_CODE_SELECT_IMAGE = 4;
    public static final int REQUEST_CODE_STORAGE_PERMISSION = 5;

    private ImageView imageViewAddNoteMain;
    private RecyclerView noteRecyclerView;
    private final List<Note> noteList = new ArrayList<>();
    private NotesAdapter notesAdapter;
    private int noteClickedPosition = -1;
    private EditText inputSearch;
    private ImageView imageAddNote, imageAddImage, imageAddWebLink;
    private AlertDialog dialogAddURL;

    private void addControlls()
    {
        imageViewAddNoteMain = findViewById(R.id.imageAddNoteMain);
        noteRecyclerView = findViewById(R.id.notesRecyclerView);
        inputSearch = findViewById(R.id.inputSearch);
        imageAddNote = findViewById(R.id.imageAddNote);
        imageAddImage = findViewById(R.id.imageAddImage);
        imageAddWebLink = findViewById(R.id.imageAddWebLink);

        noteRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2,StaggeredGridLayoutManager.VERTICAL));

        notesAdapter = new NotesAdapter(noteList,this);
        noteRecyclerView.setAdapter(notesAdapter);
    }

    @Override
    public void onNoteClicked(Note note, int position) {
        noteClickedPosition = position;
        Intent intent = new Intent(getApplicationContext(), CreatNoteActivity.class);
        intent.putExtra("isViewOrUpdate",true);
        intent.putExtra("note",note);
        startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTE);
    }

    private void getNotes(final int requestCode, final boolean isNoteDeleted)
    {
        class GetNoteTask extends AsyncTask<Void, Void, List<Note>>
        {
            @Override
            protected List<Note> doInBackground(Void... voids) {
                return NotesDatabase.getDatabase(getApplicationContext()).noteDao().getAllnotes();
            }

            @Override
            protected void onPostExecute(List<Note> notes) {
                super.onPostExecute(notes);
                if(requestCode == REQUEST_CODE_SHOW_NOTE)
                {
                    noteList.addAll(notes);
                    notesAdapter.notifyDataSetChanged();
                }
                else if(requestCode == REQUEST_CODE_ADD_NOTE)
                    {
                        noteList.add(0,notes.get(0));
                        notesAdapter.notifyItemInserted(0);
                        noteRecyclerView.smoothScrollToPosition(0);
                    }
                else if(requestCode == REQUEST_CODE_UPDATE_NOTE)
                        {
                            noteList.remove(noteClickedPosition);

                            if(isNoteDeleted)
                            {
                                notesAdapter.notifyItemRemoved(noteClickedPosition);
                            }
                            else
                            {
                                noteList.add(noteClickedPosition, notes.get(noteClickedPosition));
                                notesAdapter.notifyItemChanged(noteClickedPosition);
                            }
                        }
            }
        }
        new GetNoteTask().execute();
    }

    private void addEvents()
        {
            imageViewAddNoteMain.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                   startActivityForResult(
                           new Intent(getApplicationContext(), CreatNoteActivity.class),
                           REQUEST_CODE_ADD_NOTE
                   );
                }
            });

            inputSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    notesAdapter.cancelTime();
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    if (noteList.size() != 0)
                    {
                        notesAdapter.searchNote(editable.toString());
                    }
                }
            });

            imageAddNote.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivityForResult(
                            new Intent(getApplicationContext(), CreatNoteActivity.class),
                            REQUEST_CODE_ADD_NOTE
                    );
                }
            });

            imageAddImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
                    {
                        ActivityCompat.requestPermissions(MainActivity.this,new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},REQUEST_CODE_STORAGE_PERMISSION);
                    }
                    else
                    {
                        selectImage();
                    }
                }
            });

            imageAddWebLink.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showAddURLDialog();
                }
            });
        }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length > 0)
        {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                selectImage();
            }
            else
            {
                Toast.makeText(this, "Bạn đã từ chối quyền truy cập", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getPathFormUri(Uri contenuri)
    {
        String filePath;
        Cursor cursor = getContentResolver().query(contenuri,null,null,null,null);
        if(cursor == null)
        {
            filePath = contenuri.getPath();
        }
        else
        {
            cursor.moveToFirst();
            int i = cursor.getColumnIndex("_data");
            filePath = cursor.getString(i);
            cursor.close();
        }
        return filePath;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK)
        {
            getNotes(REQUEST_CODE_ADD_NOTE, false);
        }
        else
            if(requestCode == REQUEST_CODE_UPDATE_NOTE && resultCode == RESULT_OK)
            {
                if(data != null)
                {
                    getNotes(REQUEST_CODE_UPDATE_NOTE, data.getBooleanExtra("isNoteDelete",false));
                }
            }
            else
                if(requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK)
                {
                    if(data != null)
                    {
                        Uri selectedImageUri = data.getData();
                        if(selectedImageUri != null)
                        {
                            try {
                                String selectedImagePath = getPathFormUri(selectedImageUri);
                                Intent intent = new Intent(getApplicationContext(),CreatNoteActivity.class);
                                intent.putExtra("isFromQuickActions",true);
                                intent.putExtra("quickActionType","image");
                                intent.putExtra("imagePath",selectedImagePath);
                                startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
                            }catch (Exception e)
                            {
                                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
    }

    private void selectImage()
    {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if(intent.resolveActivity(getPackageManager())!= null)
        {
            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
        }
    }

    private void showAddURLDialog() {
        android.app.AlertDialog.Builder builder = null;
        View view = null;
        if (dialogAddURL == null) {
            builder = new android.app.AlertDialog.Builder(MainActivity.this);
            view = LayoutInflater.from(this).inflate(R.layout.layout_add_url, (ViewGroup) findViewById(R.id.layoutAddUrlContainer));
            builder.setView(view);

            dialogAddURL = builder.create();
            if(dialogAddURL.getWindow() != null)
            {
                dialogAddURL.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            final EditText inputURL = view.findViewById(R.id.inputURL);
            inputURL.requestFocus();

            view.findViewById(R.id.textAdd).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(inputURL.getText().toString().trim().isEmpty())
                    {
                        Toast.makeText(MainActivity.this, "Nhập URL", Toast.LENGTH_SHORT).show();
                    }
                    else
                    if (!Patterns.WEB_URL.matcher(inputURL.getText().toString()).matches())
                    {
                        Toast.makeText(MainActivity.this, "Nhập URL hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        dialogAddURL.dismiss(); // đóng dialog
                        Intent intent = new Intent(getApplicationContext(),CreatNoteActivity.class);
                        intent.putExtra("isFromQuickActions",true);
                        intent.putExtra("quickActionType","URL");
                        intent.putExtra("URL",inputURL.getText().toString());
                        startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
                    }
                }
            });

            view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialogAddURL.dismiss();
                }
            });
        }
        dialogAddURL.show();
    }
}