package com.doplgangr.secrecy.fragments;

import android.app.Activity;
import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.doplgangr.secrecy.R;
import com.doplgangr.secrecy.activities.FileChooserActivity;
import com.doplgangr.secrecy.adapters.FileChooserAdapter;
import com.doplgangr.secrecy.utils.Util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A fragment to display a file chooser interface
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnFileChosen}
 * interface to catch navigation and file choosing actions
 */
public class FileChooserFragment extends Fragment implements AbsListView.OnItemClickListener {

    /**
     * Fragment parameter ROOT
     * String, denotes absolute path of the file root to display
     */
    private static final String ROOT = "root";
    /**
     * Fragment parameter FOLDERS_ONLY
     * Boolean, if only folders should be displayed on the interface
     */
    private static final String FOLDERS_ONLY = "folders_only";

    private List<File> ITEMS = new ArrayList<>();

    private OnFileChosen mListener;

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private ListAdapter mAdapter;

    /**
     * The location of directory where views are displaying.
     */
    private File rootFile;

    /**
     *  If only folders should be displayed. Additional menu icon "OK"
     *  Will be displayed to allow confirming. Additional add folder icon
     *  will also be added.
     */
    private Boolean foldersOnly;


    /**
     * The extensions that will be displayed.
     */
    private ArrayList<String> fileExtensions;

    public static FileChooserFragment newInstance(File root, Boolean foldersOnly, ArrayList<String> fileExtensions) {
        FileChooserFragment fragment = new FileChooserFragment();
        Bundle args = new Bundle();

        // Default implementation: accessing the internal SDcard.
        if (root!=null)
            args.putString(ROOT, root.getAbsolutePath());
        else
            args.putString(ROOT, Environment.getExternalStorageDirectory().getAbsolutePath());

        // Default implementation: displaying all files and directory
        if (foldersOnly!=null)
            args.putBoolean(FOLDERS_ONLY, foldersOnly);
        else
            args.putBoolean(FOLDERS_ONLY, false);

        args.putStringArrayList(FileChooserActivity.FILE_EXTENSIONS, fileExtensions);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FileChooserFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            rootFile = new File(getArguments().getString(ROOT));
            foldersOnly = getArguments().getBoolean(FOLDERS_ONLY, false);
            fileExtensions = getArguments().getStringArrayList(FileChooserActivity.FILE_EXTENSIONS);
        }else{
            rootFile = Environment.getExternalStorageDirectory();
            foldersOnly = false;
            fileExtensions = null;
        }

        // If a parent exists, add the parent to the list of children so that users can traverse up.
        if (!Environment.getRootDirectory().equals(rootFile.getAbsoluteFile()))
            if (rootFile.getParentFile()!=null)
                    ITEMS.add(rootFile.getParentFile());

        // Lists all children, optionally display only folders.
        File[] filesListed = rootFile.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                File file = new File(current, name);
                if (foldersOnly)
                    return file.isDirectory();
                if (fileExtensions!=null && file.isFile())
                    return fileExtensions.contains(FilenameUtils.getExtension(name));
                return true;
            }
        });

        // Sort files by name, folder has priority
        Arrays.sort(filesListed, new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {
                if (file1.isDirectory() && !file2.isDirectory())
                    return -1;
                if (!file1.isDirectory() && file2.isDirectory())
                    return 1;
                return file1.getName().compareTo(file2.getName());
            }
        });

        Collections.addAll(ITEMS,filesListed);

        mAdapter = new FileChooserAdapter(getActivity(), ITEMS, rootFile);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (foldersOnly)
            inflater.inflate(R.menu.file_chooser,menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_ok:
                mListener.onFileSelected(rootFile,true);
                return true;
            case R.id.action_add_folder:
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),R.style.AppCompatAlertDialog);
                View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_single_edit_text,null);
                final EditText editText = (EditText) view.findViewById(R.id.editText1);
                editText.setHint(R.string.Chooser__new_folder_name_hint);
                builder.setTitle(R.string.Chooser__add_folder);
                builder.setView(view);
                builder.setCancelable(true);
                builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        File subDirectory = new File(rootFile,editText.getText().toString());
                        if (subDirectory.mkdir())
                            mListener.onFileSelected(subDirectory,false);
                    }
                });
                builder.create().show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filechooser, container, false);
        mListView = (AbsListView) view.findViewById(android.R.id.list);

        // Set the adapter
        mListView.setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        // Set the toolbar to have location name
        getActivity().setTitle(rootFile.getAbsolutePath());

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFileChosen) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFileChosen");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            if (Util.canReadDir(ITEMS.get(position)))
                mListener.onFileSelected(ITEMS.get(position),false);
            else
                mListener.onFileSelected(ITEMS.get(position),true);
        }
    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFileChosen {
        void onFileSelected(File path, Boolean confirmed);
    }
}