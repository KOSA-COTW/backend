package cotw.server.domain.board.exception;

import lombok.Getter;

@Getter
public class BoardException extends RuntimeException {
  private final String errorCode;

  public BoardException(String message){
    super(message);
    this.errorCode = "BOARD_ERROR";
  }

  public BoardException(String message,String errorCode) {
      super(message);
      this.errorCode = errorCode;
  }
}
