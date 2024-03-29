package sem.controller;

import java.text.ParseException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.validation.Valid;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.i18n.LocaleContextHolder;

import sem.dto.CurrentAccountDTO;
import sem.dto.JwtDTO;
import sem.dto.LoginDTO;
import sem.dto.Message;
import sem.dto.UserDTO;
import sem.enums.RoleName;
import sem.jwt.JwtProvider;
import sem.model.CurrentAccount;
import sem.model.History;
import sem.model.Role;
import sem.model.User;
import sem.serviceImpl.CurrentAccountService;
import sem.serviceImpl.RoleService;
import sem.serviceImpl.UserService;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping(value = "/user", produces = MediaType.APPLICATION_JSON_VALUE)

public class UserController {
	
	private Logger logger = LoggerFactory.getLogger(UserController.class);
	
	@Autowired
	private UserService userService;
	@Autowired
	private CurrentAccountService acountService;
	@Autowired
	private ModelMapper modelMapper;
	// token:
	@Autowired
	PasswordEncoder passwordEncoder;
	@Autowired
	AuthenticationManager authenticationManager;
	@Autowired
	RoleService roleService;
	@Autowired
	JwtProvider jwtProvider;
	@Autowired
	HistoryController historyController;
	@Autowired
	private MessageSource msg;

	@PostMapping
	public ResponseEntity<?> create(@Valid @RequestBody UserDTO userDTO, BindingResult result) {
		this.logger.debug("executing UserController._create()");
		try {

		// validaciones:

		if (result.hasErrors()) {
			this.logger.debug("there are errors in the validations, error:"+ result.getFieldError().getDefaultMessage());
			return new ResponseEntity<Message>(new Message(result.getFieldError().getDefaultMessage()),
					HttpStatus.BAD_REQUEST);
		}
		if (userService.existsByPhone(userDTO.getPhone())) {
			this.logger.debug("There is a user with the same phone:");
			return new ResponseEntity<Message>(
					new Message(msg.getMessage("user.existPhone", null, LocaleContextHolder.getLocale())),
					HttpStatus.BAD_REQUEST);
		}
		if (userService.existsByMail(userDTO.getMail())){
			this.logger.debug("There is a user with the same mail:");
			return new ResponseEntity<Message>(
					new Message(msg.getMessage("user.existEmail", null, LocaleContextHolder.getLocale())),
					HttpStatus.BAD_REQUEST);
		}
		// convert DTO to entity
		User userRequest = modelMapper.map(userDTO, User.class);

		// creo un user con la pass hasheada
		User user = new User(passwordEncoder.encode(userRequest.getPassword()), userRequest.getMail(),
				userRequest.getPhone(), userRequest.getUsername());

		// agrego rol y guardo el user
		Set<Role> roles = new HashSet<>();
		roles.add(roleService.getByRoleName(RoleName.ROLE_USER).get());
		user.setRoles(roles);
		userService.save(user);

		// obtengo el id de user
		Optional<User> userId = userService.findById(user.getId());
		// creo el object account;
		CurrentAccount account = new CurrentAccount(userId.get().getPhone(), 0);
		account.setUser(user);
		acountService.save(account);
		// convert entity to DTO
		CurrentAccountDTO accountReponse = modelMapper.map(account, CurrentAccountDTO.class);

		UserDTO userResponse = modelMapper.map(user, UserDTO.class);
		userResponse.setCurrentAccount(accountReponse);

		return new ResponseEntity<Message>(
				new Message(msg.getMessage("user.create", null, LocaleContextHolder.getLocale())), HttpStatus.OK);

		}
		catch (Exception e) {
	        e.printStackTrace();
	        this.logger.error("Error found: {}", e);
	        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	      
	    }
		
	}

	@PostMapping("/authenticate")
	public ResponseEntity<?> authenticate(@Valid @RequestBody LoginDTO loginDto, BindingResult result) {
		this.logger.debug("executing UserController._authenticathe()");
		try {
		// validaciones:
		if (result.hasErrors()) {
			this.logger.debug("the username/password is incorrect:");
			return new ResponseEntity<Message>(
					new Message(msg.getMessage("user.notValid", null, LocaleContextHolder.getLocale())),
					HttpStatus.BAD_REQUEST);
		}

		Authentication authentication = authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(loginDto.getPhone(), loginDto.getPassword()));

		SecurityContextHolder.getContext().setAuthentication(authentication);
		String jwt = jwtProvider.generateToken(authentication);
		JwtDTO jwtDto = new JwtDTO(jwt);

		return new ResponseEntity<JwtDTO>(jwtDto, HttpStatus.OK);
		}
		catch (Exception e) {
	        e.printStackTrace();
	        this.logger.error("Error found: {}", e);
	        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	      
	    }
		
	}

	// Read all users
	@GetMapping
	public ResponseEntity<?> getAll() {
		this.logger.debug("executing UserController._getAll()");
		try {
			return ResponseEntity.ok(userService.findAll());
		}
		catch (Exception e) {
	        e.printStackTrace();
	        this.logger.error("Error found: {}", e);
	        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	      
	    }
	}

	// Read an user
	@GetMapping("/{id}")
	public ResponseEntity<?> findById(@PathVariable(value = "id") Long userId) {
		this.logger.debug("executing UserController._findById()");
		try {
		Optional<User> user = userService.findById(userId);

		// convert entity to DTO
		UserDTO userResponse = modelMapper.map(user.get(), UserDTO.class);
		if (user.isPresent()) {
			this.logger.debug("the user with that id exists"); 
			return ResponseEntity.ok(userResponse);
		} else {
			this.logger.debug("The user with that id does not exist");
			return ResponseEntity.notFound().build();
		}
		}
		catch (Exception e) {
	        e.printStackTrace();
	        this.logger.error("Error found: {}", e);
	        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	      
	    }

	}

	@GetMapping("/currentAccount/{id}")
	public ResponseEntity<?> findCurrentAccountById(@PathVariable(value = "id") Long userId) {
		this.logger.debug("executing UserController._findCurrentAccountById()");
		try {
		Optional<User> user = userService.findById(userId);

		// convert entity to DTO
		UserDTO userResponse = modelMapper.map(user.get(), UserDTO.class);
		if (user.isPresent()) {
			this.logger.debug("the user with that id exists"); 
			return ResponseEntity.ok(userResponse.getCurrentAccount());
		} else {
			this.logger.debug("The user with that id does not exist");
			return ResponseEntity.notFound().build();
		}
		}
		catch (Exception e) {
	        e.printStackTrace();
	        this.logger.error("Error found: {}", e);
	        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	      
	    }
	}

	// Update an User
	@PutMapping("/account/{id}")
	public ResponseEntity<?> updateAmount(@Valid @RequestBody CurrentAccountDTO accountDTO, BindingResult result,
			@PathVariable(value = "id") Long userId) {
		
		this.logger.debug("executing UserController._updateAmount()");
		try {
		// validaciones:
		if (result.hasErrors()) {
			this.logger.debug("there are errors in the validations, error:"+ result.getFieldError().getDefaultMessage());
			return new ResponseEntity<Message>(new Message(result.getFieldError().getDefaultMessage()),
					HttpStatus.BAD_REQUEST);
		}

		Optional<User> user = userService.findByPhone(accountDTO.getPhone());

		double amount = accountDTO.getBalance();
		// actualizo la cuenta:
		user.get().getCurrentAccount()
				.setBalance(user.get().getCurrentAccount().getBalance() + amount);

		if (user.isEmpty()) {
			this.logger.debug("The user with that account does not exist"); 
			return ResponseEntity.notFound().build();
		} else {
			this.logger.debug("the user with that account exists"); 
			// si todo sale bien debugrme exitosamente el resultado
			userService.updateAccount(user.get());

			History history = new History("Credit Load", accountDTO.getBalance(), user.get().getCurrentAccount().getBalance(),
					user.get().getCurrentAccount());
			historyController.create(history);

			return new ResponseEntity<Message>(
					new Message(msg.getMessage("user.update.amount", new String[] { String.valueOf(amount) },
							LocaleContextHolder.getLocale())),
					HttpStatus.OK);
		}
		}
		catch (Exception e) {
	        e.printStackTrace();
	        this.logger.error("Error found: {}", e);
	        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	      
	    }

	}

	@PostMapping("/refresh")
	public ResponseEntity<?> refresh(@RequestBody JwtDTO jwtDto) throws ParseException {
		this.logger.debug("executing UserController._refresh()");
		try {
		String token = jwtProvider.refreshToken(jwtDto);
		JwtDTO jwt = new JwtDTO(token);
		return new ResponseEntity<JwtDTO>(jwt, HttpStatus.OK);
		}
		catch (Exception e) {
	        e.printStackTrace();
	        this.logger.error("Error found: {}", e);
	        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	      
	    }
		}

}
