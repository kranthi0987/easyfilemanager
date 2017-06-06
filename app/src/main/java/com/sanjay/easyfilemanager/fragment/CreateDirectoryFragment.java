/*
 * Copyright (C) 2014 Hari Krishna Dulipudi
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sanjay.easyfilemanager.fragment;

import android.app.Dialog;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.sanjay.easyfilemanager.BaseActivity;
import com.sanjay.easyfilemanager.DialogFragment;
import com.sanjay.easyfilemanager.DocumentsApplication;
import com.sanjay.easyfilemanager.R;
import com.sanjay.easyfilemanager.misc.AsyncTask;
import com.sanjay.easyfilemanager.misc.ContentProviderClientCompat;
import com.sanjay.easyfilemanager.misc.CrashReportingManager;
import com.sanjay.easyfilemanager.misc.ProviderExecutor;
import com.sanjay.easyfilemanager.misc.Utils;
import com.sanjay.easyfilemanager.model.DocumentInfo;
import com.sanjay.easyfilemanager.model.DocumentsContract;
import com.sanjay.easyfilemanager.model.DocumentsContract.Document;

import static com.sanjay.easyfilemanager.DocumentsActivity.TAG;

/**
 * Dialog to create a new directory.
 */
public class CreateDirectoryFragment extends DialogFragment {
    private static final String TAG_CREATE_DIRECTORY = "create_directory";

    public static void show(FragmentManager fm) {
        final CreateDirectoryFragment dialog = new CreateDirectoryFragment();
        dialog.show(fm, TAG_CREATE_DIRECTORY);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());

        final View view = dialogInflater.inflate(R.layout.dialog_create_dir, null, false);
        final EditText text1 = (EditText) view.findViewById(android.R.id.text1);
        Utils.tintWidget(text1);

        builder.setTitle(R.string.menu_create_dir);
        builder.setView(view);

        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String displayName = text1.getText().toString();

                final BaseActivity activity = (BaseActivity) getActivity();
                final DocumentInfo cwd = activity.getCurrentDirectory();

                if(TextUtils.isEmpty(displayName)){
                    activity.showError(R.string.create_error);
                    return;
                }
                new CreateDirectoryTask(activity, cwd, displayName).executeOnExecutor(
                        ProviderExecutor.forAuthority(cwd.authority));
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }
    
    private class CreateDirectoryTask extends AsyncTask<Void, Void, DocumentInfo> {
        private final BaseActivity mActivity;
        private final DocumentInfo mCwd;
		private final String mDisplayName;

        public CreateDirectoryTask(
                BaseActivity activity, DocumentInfo cwd, String displayName) {
            mActivity = activity;
            mCwd = cwd;
            mDisplayName = displayName;
        }

        @Override
        protected void onPreExecute() {
            mActivity.setPending(true);
        }

        @Override
        protected DocumentInfo doInBackground(Void... params) {
            final ContentResolver resolver = mActivity.getContentResolver();
            ContentProviderClient client = null;
            try {
				client = DocumentsApplication.acquireUnstableProviderOrThrow(resolver, mCwd.derivedUri.getAuthority());
                final Uri childUri = DocumentsContract.createDocument(
                		resolver, mCwd.derivedUri, Document.MIME_TYPE_DIR, mDisplayName);
                return DocumentInfo.fromUri(resolver, childUri);
            } catch (Exception e) {
                Log.w(TAG, "Failed to create directory", e);
                CrashReportingManager.logException(e);
                return null;
            } finally {
            	ContentProviderClientCompat.releaseQuietly(client);
            }
        }

        @Override
        protected void onPostExecute(DocumentInfo result) {
            if (result != null) {
                // Navigate into newly created child
                mActivity.onDocumentPicked(result);
            } else {
                if(!mActivity.isSAFIssue(mCwd.documentId)) {
                    mActivity.showError(R.string.create_error);
                }
            }

            mActivity.setPending(false);
        }
    }
}
