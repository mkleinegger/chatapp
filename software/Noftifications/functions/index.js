'use strict'

const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);

exports.sendNotification = functions.database.ref('/Notifications/{user_id}/{notification_id}').onWrite((change, context) => {
    let user_id = context.params.user_id;
    let notification_id = context.params.notification_id;

    console.log('We have notification to send to : ', user_id);

    if (!change.after.exists()) {
        console.log('A notification has been deleted from the database : ', notification_id);
        return
    }

    let fromUser = admin.database().ref(`/Notifications/${user_id}/${notification_id}`).once('value');
    return fromUser.then(fromUserResult => {
        let sender_id = fromUserResult.val().sender;
        let message = fromUserResult.val().message;

        let userQuery = admin.database().ref(`/Users/${sender_id}/fullname`).once('value');
        return userQuery.then(userQueryResult => {
            let name = userQueryResult.val();

            let deviceToken = admin.database().ref(`/Users/${user_id}/token`).once('value');
            return deviceToken.then(result => {
                let token_id = result.val();
                let payload = {
                    notification: {
                        title: `${name}`,
                        body: `${message}`,
                        icon: 'default'
                    }
                };

                return admin.messaging().sendToDevice(token_id, payload).then(response => {
                    console.log(`The notification has been sent to ${token_id} with ${payload}`);
                });
            });
        });

    });
});