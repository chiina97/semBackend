package sem.dto;

import java.io.Serializable;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class LoginDTO implements Serializable {

	private static final long serialVersionUID = 1L;
	@NotNull(message = "{user.password.notNull}")
	private String password;
	@NotNull(message = "{user.phone.notNull}")
	@NotBlank(message = "{user.phone.notBlank}")
	@Size(min = 10, max = 10, message = "{user.phone.notSize}")
	@Pattern(regexp = "[0-9]+", message = "{user.phone.notNumber}")
	private String phone;

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

}
