package com.tuan.appnote.listeners;

import com.tuan.appnote.entities.Note;

public interface NotesListener {
    void onNoteClicked(Note note, int position);
}
