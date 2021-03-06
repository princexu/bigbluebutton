import React, { memo } from 'react';
import { withTracker } from 'meteor/react-meteor-data';
import UserListService from '/imports/ui/components/user-list/service';
import Settings from '/imports/ui/services/settings';
import ChatAlert from './component';
import ChatService from '/imports/ui/components/chat/service.js';
import Auth from '/imports/ui/services/auth';
import Users from '/imports/api/users';

const ChatAlertContainer = props => (
  <ChatAlert {...props} />
);

export default withTracker(() => {
  const AppSettings = Settings.application;
  const activeChats = UserListService.getActiveChats();
  const loginTime = Users.findOne({ userId: Auth.userID }).loginTime;
  return {
    audioAlertDisabled: !AppSettings.chatAudioAlerts,
    pushAlertDisabled: !AppSettings.chatPushAlerts,
    activeChats,
    publicUserId: Meteor.settings.public.chat.public_group_id,
    joinTimestamp: loginTime,
  };
})(memo(ChatAlertContainer));
