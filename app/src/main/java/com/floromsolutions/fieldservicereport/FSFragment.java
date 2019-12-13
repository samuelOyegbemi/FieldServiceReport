package com.floromsolutions.fieldservicereport;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
   public  class FSFragment extends Fragment {
       public static final String ARG_LAYOUT = "layout_id";
       public static final String ARG_ACB_TITLE_RES_ID = "action_bar_title";
       public static final String ARG_MENU_ID = "menu_id";


       public interface OnCompleteListener{
            void onFragmentReady(int layoutId);
            void onFragmentPaused(int layoutId);
        }

        private OnCompleteListener fragListener;

        @SuppressWarnings("deprecation")
        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            _onAttach(activity);
        }

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
            _onAttach(context);
        }

        private void _onAttach(Context context){
            try
            {
                this.fragListener = (OnCompleteListener)context;
            }
            catch (final ClassCastException e){
                throw new ClassCastException(context.toString());
            }
        }

        public static FSFragment newInstance(int layoutId, int actBarTitleResId, int belongToMenuId){
            FSFragment fsFragment = new FSFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_LAYOUT, layoutId);
            args.putInt(ARG_ACB_TITLE_RES_ID, actBarTitleResId);
            args.putInt(ARG_MENU_ID, belongToMenuId);

            fsFragment.setArguments(args);
            return fsFragment;
        }

        public FSFragment() {
            // Empty constructor required for fragment subclasses
        }

        @Override

        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            int FragId = getArguments().getInt(ARG_LAYOUT);
            return inflater.inflate(FragId, container, false);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            fragListener.onFragmentReady(getArguments().getInt(ARG_LAYOUT));
        }

       @Override
       public void onPause() {
           super.onPause();
           fragListener.onFragmentPaused(getArguments().getInt(ARG_LAYOUT));
       }
   }
