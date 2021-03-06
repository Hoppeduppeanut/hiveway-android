/*
 * Copyright 2018 Hiveway
 * Copyright 2017 Andrew Dawson
 *
 * This file is a part of Tusky and Hiveway.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Tusky is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Tusky; if not,
 * see <http://www.gnu.org/licenses>. */

package org.hiveway.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.PopupMenu;
import android.text.Spanned;
import android.view.MenuItem;
import android.view.View;

import org.hiveway.AccountActivity;
import org.hiveway.BaseActivity;
import org.hiveway.ComposeActivity;
import org.hiveway.R;
import org.hiveway.ReportActivity;
import org.hiveway.HivewayApplication;
import org.hiveway.ViewMediaActivity;
import org.hiveway.ViewTagActivity;
import org.hiveway.ViewThreadActivity;
import org.hiveway.ViewVideoActivity;
import org.hiveway.db.AccountEntity;
import org.hiveway.entity.Attachment;
import org.hiveway.entity.Relationship;
import org.hiveway.entity.Status;
import org.hiveway.interfaces.AdapterItemRemover;
import org.hiveway.network.HivewayApi;
import org.hiveway.receiver.TimelineReceiver;
import org.hiveway.util.HtmlUtils;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/* Note from Andrew on Jan. 22, 2017: This class is a design problem for me, so I left it with an
 * awkward name. TimelineFragment and NotificationFragment have significant overlap but the nature
 * of that is complicated by how they're coupled with Status and Notification and the corresponding
 * adapters. I feel like the profile pages and thread viewer, which I haven't made yet, will also
 * overlap functionality. So, I'm momentarily leaving it and hopefully working on those will clear
 * up what needs to be where. */
public abstract class SFragment extends BaseFragment implements AdapterItemRemover {
    protected static final int COMPOSE_RESULT = 1;

    protected String loggedInAccountId;
    protected String loggedInUsername;
    protected HivewayApi hivewayApi;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AccountEntity activeAccount = HivewayApplication.getAccountManager().getActiveAccount();
        if(activeAccount != null) {
            loggedInAccountId = activeAccount.getAccountId();
            loggedInUsername = activeAccount.getUsername();
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        BaseActivity activity = (BaseActivity) getActivity();
        hivewayApi = activity.hivewayApi;
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        getActivity().overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left);
    }

    protected void reply(Status status) {
        String inReplyToId = status.getActionableId();
        Status actionableStatus = status.getActionableStatus();
        Status.Visibility replyVisibility = actionableStatus.getVisibility();
        String contentWarning = actionableStatus.getSpoilerText();
        Status.Mention[] mentions = actionableStatus.getMentions();
        List<String> mentionedUsernames = new ArrayList<>();
        mentionedUsernames.add(actionableStatus.getAccount().getUsername());
        for (Status.Mention mention : mentions) {
            mentionedUsernames.add(mention.getUsername());
        }
        mentionedUsernames.remove(loggedInUsername);
        Intent intent = new ComposeActivity.IntentBuilder()
                .inReplyToId(inReplyToId)
                .replyVisibility(replyVisibility)
                .contentWarning(contentWarning)
                .mentionedUsernames(mentionedUsernames)
                .repyingStatusAuthor(actionableStatus.getAccount().getLocalUsername())
                .replyingStatusContent(actionableStatus.getContent().toString())
                .build(getContext());
        startActivityForResult(intent, COMPOSE_RESULT);
    }

    protected void reblogWithCallback(final Status status, final boolean reblog,
                                      Callback<Status> callback) {
        String id = status.getActionableId();

        Call<Status> call;
        if (reblog) {
            call = hivewayApi.reblogStatus(id);
        } else {
            call = hivewayApi.unreblogStatus(id);
        }
        call.enqueue(callback);
    }

    protected void favouriteWithCallback(final Status status, final boolean favourite,
                                         final Callback<Status> callback) {
        String id = status.getActionableId();

        Call<Status> call;
        if (favourite) {
            call = hivewayApi.favouriteStatus(id);
        } else {
            call = hivewayApi.unfavouriteStatus(id);
        }
        call.enqueue(callback);
        callList.add(call);
    }

    protected void openReblog(@Nullable final Status status) {
        if (status == null) return;
        viewAccount(status.getAccount().getId());
    }

    private void mute(String id) {
        Call<Relationship> call = hivewayApi.muteAccount(id);
        call.enqueue(new Callback<Relationship>() {
            @Override
            public void onResponse(@NonNull Call<Relationship> call, @NonNull Response<Relationship> response) {}

            @Override
            public void onFailure(@NonNull Call<Relationship> call, @NonNull Throwable t) {}
        });
        callList.add(call);
        Intent intent = new Intent(TimelineReceiver.Types.MUTE_ACCOUNT);
        intent.putExtra("id", id);
        LocalBroadcastManager.getInstance(getContext())
                .sendBroadcast(intent);
    }

    private void block(String id) {
        Call<Relationship> call = hivewayApi.blockAccount(id);
        call.enqueue(new Callback<Relationship>() {
            @Override
            public void onResponse(@NonNull Call<Relationship> call, @NonNull retrofit2.Response<Relationship> response) {}

            @Override
            public void onFailure(@NonNull Call<Relationship> call, @NonNull Throwable t) {}
        });
        callList.add(call);
        Intent intent = new Intent(TimelineReceiver.Types.BLOCK_ACCOUNT);
        intent.putExtra("id", id);
        LocalBroadcastManager.getInstance(getContext())
                .sendBroadcast(intent);
    }

    private void delete(String id) {
        Call<ResponseBody> call = hivewayApi.deleteStatus(id);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull retrofit2.Response<ResponseBody> response) {}

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {}
        });
        callList.add(call);
    }

    protected void more(final Status status, View view, final int position) {
        final String id = status.getActionableId();
        final String accountId = status.getActionableStatus().getAccount().getId();
        final String accountUsename = status.getActionableStatus().getAccount().getUsername();
        final Spanned content = status.getActionableStatus().getContent();
        final String statusUrl = status.getActionableStatus().getUrl();
        PopupMenu popup = new PopupMenu(getContext(), view);
        // Give a different menu depending on whether this is the user's own post or not.
        if (loggedInAccountId == null || !loggedInAccountId.equals(accountId)) {
            popup.inflate(R.menu.status_more);
        } else {
            popup.inflate(R.menu.status_more_for_user);
        }
        popup.setOnMenuItemClickListener(
                new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.status_share_content: {
                                StringBuilder sb = new StringBuilder();
                                sb.append(status.getAccount().getUsername());
                                sb.append(" - ");
                                sb.append(status.getContent().toString());

                                Intent sendIntent = new Intent();
                                sendIntent.setAction(Intent.ACTION_SEND);
                                sendIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
                                sendIntent.setType("text/plain");
                                startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.send_status_content_to)));
                                return true;
                            }
                            case R.id.status_share_link: {
                                Intent sendIntent = new Intent();
                                sendIntent.setAction(Intent.ACTION_SEND);
                                sendIntent.putExtra(Intent.EXTRA_TEXT, statusUrl);
                                sendIntent.setType("text/plain");
                                startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.send_status_link_to)));
                                return true;
                            }
                            case R.id.status_copy_link: {
                                ClipboardManager clipboard = (ClipboardManager)
                                        getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText(null, statusUrl);
                                clipboard.setPrimaryClip(clip);
                                return true;
                            }
                            case R.id.status_mute: {
                                mute(accountId);
                                return true;
                            }
                            case R.id.status_block: {
                                block(accountId);
                                return true;
                            }
                            case R.id.status_report: {
                                openReportPage(accountId, accountUsename, id, content);
                                return true;
                            }
                            case R.id.status_delete: {
                                delete(id);
                                removeItem(position);
                                return true;
                            }
                        }
                        return false;
                    }
                });
        popup.show();
    }

    protected void viewMedia(String[] urls, int urlIndex, Attachment.Type type,
                             @Nullable View view) {
        switch (type) {
            case IMAGE: {
                Intent intent = new Intent(getContext(), ViewMediaActivity.class);
                intent.putExtra("urls", urls);
                intent.putExtra("urlIndex", urlIndex);
                if (view != null) {
                    String url = urls[urlIndex];
                    ViewCompat.setTransitionName(view, url);
                    ActivityOptionsCompat options =
                            ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(),
                            view, url);
                    startActivity(intent, options.toBundle());
                } else {
                    startActivity(intent);
                }
                break;
            }
            case GIFV:
            case VIDEO: {
                Intent intent = new Intent(getContext(), ViewVideoActivity.class);
                intent.putExtra("url", urls[urlIndex]);
                startActivity(intent);
                break;
            }
            case UNKNOWN: {
                /* Intentionally do nothing. This case is here is to handle when new attachment
                 * types are added to the API before code is added here to handle them. So, the
                 * best fallback is to just show the preview and ignore requests to view them. */
                break;
            }
        }
    }

    protected void viewThread(Status status) {
        Intent intent = new Intent(getContext(), ViewThreadActivity.class);
        intent.putExtra("id", status.getActionableId());
        intent.putExtra("url", status.getActionableStatus().getUrl());
        startActivity(intent);
    }

    protected void viewTag(String tag) {
        Intent intent = new Intent(getContext(), ViewTagActivity.class);
        intent.putExtra("hashtag", tag);
        startActivity(intent);
    }

    protected void viewAccount(String id) {
        Intent intent = new Intent(getContext(), AccountActivity.class);
        intent.putExtra("id", id);
        startActivity(intent);
    }

    protected void openReportPage(String accountId, String accountUsername, String statusId,
            Spanned statusContent) {
        Intent intent = new Intent(getContext(), ReportActivity.class);
        intent.putExtra("account_id", accountId);
        intent.putExtra("account_username", accountUsername);
        intent.putExtra("status_id", statusId);
        intent.putExtra("status_content", HtmlUtils.toHtml(statusContent));
        startActivity(intent);
    }
}
