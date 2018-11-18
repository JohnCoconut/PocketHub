/*
 * Copyright (c) 2015 PocketHub
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.pockethub.android.ui.user;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.afollestad.materialdialogs.MaterialDialog;
import com.meisolsson.githubsdk.model.Gist;
import com.meisolsson.githubsdk.model.Issue;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.model.User;
import com.github.pockethub.android.R;
import com.github.pockethub.android.core.commit.CommitMatch;
import com.github.pockethub.android.core.commit.CommitUriMatcher;
import com.github.pockethub.android.core.gist.GistUriMatcher;
import com.github.pockethub.android.core.issue.IssueUriMatcher;
import com.github.pockethub.android.core.repo.RepositoryUriMatcher;
import com.github.pockethub.android.core.user.UserUriMatcher;
import com.github.pockethub.android.ui.commit.CommitViewActivity;
import com.github.pockethub.android.ui.gist.GistsViewActivity;
import com.github.pockethub.android.ui.issue.IssuesViewActivity;
import com.github.pockethub.android.ui.repo.RepositoryViewActivity;

import java.net.URI;
import java.text.MessageFormat;

import static android.content.Intent.ACTION_VIEW;
import static android.content.Intent.CATEGORY_BROWSABLE;

/**
 * Activity to launch other activities based on the intent's data {@link URI}
 */
public class UriLauncherActivity extends Activity {

    private static final String HOST_DEFAULT = "github.com";
    private static final String HOST_GISTS = "gist.github.com";
    private static final String PROTOCOL_HTTPS = "https";

    static public void launchUri(Context context, Uri data) {
        Intent intent = getIntentForURI(data);
        if (intent != null) {
            context.startActivity(intent);
        } else {
            context.startActivity(new Intent(ACTION_VIEW, data).addCategory(CATEGORY_BROWSABLE));
        }
    }

    /**
     * Convert global view intent one into one that can be possibly opened
     * inside the current application.
     *
     * @param intent
     * @return converted intent or null if non-application specific
     */
    static public Intent convert(final Intent intent) {
        if (intent == null) {
            return null;
        }

        if (!ACTION_VIEW.equals(intent.getAction())) {
            return null;
        }

        Uri data = intent.getData();
        if (data == null) {
            return null;
        }

        if (TextUtils.isEmpty(data.getHost()) || TextUtils.isEmpty(data.getScheme())) {
            String host = data.getHost();
            if (TextUtils.isEmpty(host)) {
                host = HOST_DEFAULT;
            }
            String scheme = data.getScheme();
            if (TextUtils.isEmpty(scheme)) {
                scheme = PROTOCOL_HTTPS;
            }
            String prefix = scheme + "://" + host;

            String path = data.getPath();
            if (!TextUtils.isEmpty(path)) {
                if (path.charAt(0) == '/') {
                    data = Uri.parse(prefix + path);
                } else {
                    data = Uri.parse(prefix + '/' + path);
                }
            } else {
                data = Uri.parse(prefix);
            }
            intent.setData(data);
        }

        return getIntentForURI(data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        final Uri data = intent.getData();

        final Intent newIntent = getIntentForURI(data);
        if (newIntent != null) {
            startActivity(newIntent);
            finish();
            return;
        }

        if (!intent.hasCategory(CATEGORY_BROWSABLE)) {
            startActivity(new Intent(ACTION_VIEW, data).addCategory(CATEGORY_BROWSABLE));
            finish();
        } else {
            showParseError(data.toString());
        }
    }

    static private Intent getIntentForURI(Uri data) {
        if (HOST_GISTS.equals(data.getHost())) {
            Gist gist = GistUriMatcher.getGist(data);
            if (gist != null) {
                return GistsViewActivity.Companion.createIntent(gist);
            }
        } else if (HOST_DEFAULT.equals(data.getHost())) {
            CommitMatch commit = CommitUriMatcher.getCommit(data);
            if (commit != null) {
                return CommitViewActivity.Companion.createIntent(commit.getRepository(), commit.getCommit());
            }

            Issue issue = IssueUriMatcher.getIssue(data);
            if (issue != null) {
                return IssuesViewActivity.Companion.createIntent(issue, issue.repository());
            }

            Repository repository = RepositoryUriMatcher.getRepository(data);
            if (repository != null) {
                return RepositoryViewActivity.Companion.createIntent(repository);
            }

            User user = UserUriMatcher.getUser(data);
            if (user != null) {
                return UserViewActivity.Companion.createIntent(user);
            }
        }

        return null;
    }

    private void showParseError(String url) {
        new MaterialDialog.Builder(this)
                .title(R.string.title_invalid_github_url)
                .content(MessageFormat.format(getString(R.string.message_invalid_github_url), url))
                .cancelListener(dialog -> finish())
                .positiveText(android.R.string.ok)
                .onPositive((dialog, which) -> finish())
                .show();
    }
}
