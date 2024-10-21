package com.parafusion.auth.service;

import com.parafusion.auth.data.model.AuthProvider;
import com.parafusion.auth.data.model.ClientType;
import com.parafusion.auth.data.model.UserToken;
import com.parafusion.auth.data.model.dto.TokenDto;
import com.parafusion.auth.data.model.User;
import com.parafusion.auth.data.repository.UserRepository;

import com.parafusion.auth.data.repository.UserTokenRepository;
import com.parafusion.auth.error.enums.ErrorEnum;
import com.parafusion.auth.error.exception.BusinessLogicException;
import com.parafusion.auth.utils.TokenUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserTokenRepository userTokenRepository;

	@Autowired
	private SessionFactory sessionFactory;

	@Autowired
	private TokenUtils tokenUtils;


	public void processOAuthPostLogin(String username) {
		User existUser = userRepository.getUserByUsername(username);
		
		if (existUser == null) {
			User newUser = new User();
			newUser.setEmail(username);
			newUser.setAuthProvider(AuthProvider.GOOGLE);
			newUser.setActivated(true);
			
			userRepository.save(newUser);
			
			System.out.println("Created new user: " + username);
		}
		
	}

	/**
	 * Temporary method. Only user for local development. Will be removed
	 * @param user
	 */
	public void saveUser(User user){
		userRepository.save(user);
	}

	public User getUserByUsernameOrEmail(String email){
		return userRepository.getUserByEmailOrUsername(email);
	}


	/**
	 * transaction is managed manually because we may save data to db and then throw BusinessLogicException
	 */
    public TokenDto autheticate(String emailUsername, String pass, ClientType clientType) {
		User user = userRepository.getUserByEmailOrUsername(emailUsername);
		if(user == null){
			throw new BusinessLogicException(ErrorEnum.LOGIN_INVALID_CREDENTIALS);
		}
		long timeToWait = Double.valueOf(Math.pow(2, user.getFailedLoginCount() - 10)).longValue() * 15;
		if(user.isLocked() && LocalDateTime.now().isBefore(user.getLastloginAttempt().plusMinutes(timeToWait))){
			throw new BusinessLogicException(ErrorEnum.LOGIN_USER_LOCKED.getCode(),"Account locked until " + user.getLastloginAttempt().plusMinutes(timeToWait));
		}

		user.setLastloginAttempt(LocalDateTime.now());
		String dbPass = user.getPassword();
		Session session = sessionFactory.openSession();
		session.beginTransaction();
		if(!BCrypt.checkpw(pass,dbPass)){
			user.setFailedLoginCount(user.getFailedLoginCount() + 1);

			if(user.getFailedLoginCount() >=5){
				user.setShowCaptcha(true);
			}

			if(user.getFailedLoginCount() >=10){
				user.setLocked(true);
				userRepository.save(user);
				session.getTransaction().commit();
				session.close();
				timeToWait = Double.valueOf(Math.pow(2, user.getFailedLoginCount() - 10)).longValue() * 15;
				throw new BusinessLogicException(ErrorEnum.LOGIN_USER_LOCKED.getCode(),"Account locked until " + user.getLastloginAttempt().plusMinutes(timeToWait));
			}
			userRepository.save(user);
			session.getTransaction().commit();
			session.close();
			throw new BusinessLogicException(ErrorEnum.LOGIN_INVALID_CREDENTIALS);

		}else{
			user.setFailedLoginCount(0);
			user.setShowCaptcha(false);
			user.setLocked(false);
			userRepository.save(user);
			session.getTransaction().commit();
			session.close();
		}

		TokenDto tokenDto = TokenDto.builder().access_token(tokenUtils.generateToken(user.getId(), clientType)).build();
		//get the previous token and invalidate if exists
		UserToken userToken = userTokenRepository.getUserTokenByUserIdAndClientTypeAndActive(user.getId(), clientType, true);
		if(userToken !=  null){
			userToken.setActive(false);
			userToken.setUpdatedDate(LocalDateTime.now());
			userTokenRepository.save(userToken);
		}
		userToken = UserToken.builder()
				.userId(user.getId())
				.clientType(clientType)
				.tokenHash("")
				.createdDate(LocalDateTime.now())
				.build();
		//register the new token and return

		return tokenDto;
	}

}
