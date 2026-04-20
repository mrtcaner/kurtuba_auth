
UPDATE public.localization_message
SET message = 'Kurtuba hesabınızda metaName değişti.', updated_date = CURRENT_TIMESTAMP
WHERE message_key = 'mail.account.modification.content.prologue' and language_code = 'tr';


insert into public.localization_message (id, message_key, language_code, message, created_date) values('65207b5f-457a-4531-aed9-7fd977b600ca','mail.account.modification.content.metaname.password','tr','şifre', CURRENT_TIMESTAMP);
insert into public.localization_message (id, message_key, language_code, message, created_date) values('40050cf5-9a54-402f-9998-621e0708b95e','mail.account.modification.content.metaname.phonenumber','tr','telefon numarası',CURRENT_TIMESTAMP);
insert into public.localization_message (id, message_key, language_code, message, created_date) values('95743a83-9c8d-4f38-b313-eb0f6349a062','mail.account.modification.content.metaname.emailaddress','tr','email adresi',CURRENT_TIMESTAMP);

insert into public.localization_message (id, message_key, language_code, message, created_date) values('75ad8dc2-095f-423e-87a8-fdb5262c2fc9','mail.account.modification.content.metaname.password','en','password',CURRENT_TIMESTAMP);
insert into public.localization_message (id, message_key, language_code, message, created_date) values('c8f7bc1b-8fee-4543-9e7d-ec479a011792','mail.account.modification.content.metaname.phonenumber','en','mobile number',CURRENT_TIMESTAMP);
insert into public.localization_message (id, message_key, language_code, message, created_date) values('f81cc2a7-2360-4114-9faa-121e1b53ce78','mail.account.modification.content.metaname.emailaddress','en','email address',CURRENT_TIMESTAMP);