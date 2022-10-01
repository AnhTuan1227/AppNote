package com.tuan.appnote.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

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
        getNotes();
    }

    public static final int REQUEST_CODE_ADD_NOTE = 1;
    public static final int REQUEST_CODE_UPDATE_NOTE = 2;
    private ImageView imageViewAddNoteMain;
    private RecyclerView noteRecyclerView;
    private List<Note> noteList = new ArrayList<>();
    private NotesAdapter notesAdapter;
    private int noteClickedPosition = -1;
    private void addControlls()
    {
        imageViewAddNoteMain = findViewById(R.id.imageAddNoteMain);
        noteRecyclerView = findViewById(R.id.notesRecyclerView);

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

    private void getNotes()
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
                if(noteList.size() == 0)
                {
                    noteList.addAll(notes);
                    notesAdapter.notifyDataSetChanged();
                }
                else
                {
                    noteList.add(0,notes.get(0)); // list note k trống thì add note mới nhất vào list và thông báo mới cho adapter rồi cuộn view người xem lên đầu
                    notesAdapter.notifyItemInserted(0);
                }
                noteRecyclerView.smoothScrollToPosition(0);
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
        }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK)
        {
            getNotes();
        }
    }
}