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

package org.hiveway.adapter;

import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.hiveway.R;
import org.hiveway.entity.Card;
import org.hiveway.entity.Status;
import org.hiveway.interfaces.StatusActionListener;
import org.hiveway.util.CustomURLSpan;
import org.hiveway.util.LinkHelper;
import org.hiveway.viewdata.StatusViewData;
import com.squareup.picasso.Picasso;

import org.hiveway.util.LinkHelper;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;

class StatusDetailedViewHolder extends StatusBaseViewHolder {
    private TextView reblogs;
    private TextView favourites;
    private LinearLayout cardView;
    private LinearLayout cardInfo;
    private ImageView cardImage;
    private TextView cardTitle;
    private TextView cardDescription;
    private TextView cardUrl;

    StatusDetailedViewHolder(View view) {
        super(view);
        reblogs = view.findViewById(R.id.status_reblogs);
        favourites = view.findViewById(R.id.status_favourites);
        cardView = view.findViewById(R.id.card_view);
        cardInfo = view.findViewById(R.id.card_info);
        cardImage = view.findViewById(R.id.card_image);
        cardTitle = view.findViewById(R.id.card_title);
        cardDescription = view.findViewById(R.id.card_description);
        cardUrl = view.findViewById(R.id.card_link);
    }

    @Override
    protected int getMediaPreviewHeight(Context context) {
        return context.getResources().getDimensionPixelSize(R.dimen.status_detail_media_preview_height);
    }

    @Override
    protected void setCreatedAt(@Nullable Date createdAt) {
        if (createdAt != null) {
            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT);
            timestampInfo.setText(dateFormat.format(createdAt));
        } else {
            timestampInfo.setText("");
        }
    }

    private void setApplication(@Nullable Status.Application app) {
        if (app != null) {

            timestampInfo.append("  •  ");

            if (app.getWebsite() != null) {
                URLSpan span = new CustomURLSpan(app.getWebsite());

                SpannableStringBuilder text = new SpannableStringBuilder(app.getName());
                text.setSpan(span, 0, app.getName().length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                timestampInfo.append(text);
                timestampInfo.setMovementMethod(LinkMovementMethod.getInstance());
            } else {
                timestampInfo.append(app.getName());
            }
        }
    }

    @Override
    void setupWithStatus(final StatusViewData.Concrete status, final StatusActionListener listener,
                         boolean mediaPreviewEnabled) {
        super.setupWithStatus(status, listener, mediaPreviewEnabled);

        NumberFormat numberFormat = NumberFormat.getNumberInstance();

        reblogs.setText(numberFormat.format(status.getReblogsCount()));
        favourites.setText(numberFormat.format(status.getFavouritesCount()));
        setApplication(status.getApplication());

        if(status.getAttachments().length == 0 && status.getCard() != null && !TextUtils.isEmpty(status.getCard().getUrl())) {
            final Card card = status.getCard();
            cardView.setVisibility(View.VISIBLE);
            cardTitle.setText(card.getTitle());
            cardDescription.setText(card.getDescription());

            cardUrl.setText(card.getUrl());

            if(card.getWidth() > 0 && card.getHeight() > 0 && !TextUtils.isEmpty(card.getImage())) {
                cardImage.setVisibility(View.VISIBLE);

                if(card.getWidth() > card.getHeight()) {
                    cardView.setOrientation(LinearLayout.VERTICAL);
                    cardImage.getLayoutParams().height = cardImage.getContext().getResources()
                            .getDimensionPixelSize(R.dimen.card_image_vertical_height);
                    cardImage.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
                    cardInfo.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                    cardInfo.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
                } else {
                    cardView.setOrientation(LinearLayout.HORIZONTAL);
                    cardImage.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                    cardImage.getLayoutParams().width = cardImage.getContext().getResources()
                            .getDimensionPixelSize(R.dimen.card_image_horizontal_width);
                    cardInfo.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    cardInfo.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    cardView.setClipToOutline(true);
                }

                Picasso.with(cardImage.getContext())
                        .load(card.getImage())
                        .fit()
                        .centerCrop()
                        .into(cardImage);

            } else {
                cardImage.setVisibility(View.GONE);
            }

            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    LinkHelper.openLink(card.getUrl(), v.getContext());

                }

            });

        } else {
            cardView.setVisibility(View.GONE);
        }


    }
}