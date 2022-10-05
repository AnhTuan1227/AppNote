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
        getNotes(REQUEST_CODE_SHOW_NOTE, false);
    }
    public static final int REQUEST_CODE_SHOW_NOTE = 3;
    public static final int REQUEST_CODE_ADD_NOTE = 1;
    public static final int REQUEST_CODE_UPDATE_NOTE = 2;
    private ImageView imageViewAddNoteMain;
    private RecyclerView noteRecyclerView;
    private final List<Note> noteList = new ArrayList<>();
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
    }


}