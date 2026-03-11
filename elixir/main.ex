# Implementation here!
defmodule FourRings do
    def start(n, h) do
        IO.puts("N = #{n}")
        IO.puts("H = #{h}")

        # prompt for input
        input = IO.gets("Enter a number: \n")
        {n, _} = Integer.parse(input)
        cond do
            input == "done" ->
                # stop accepting new input
                IO.puts("Shutting down")
            n < 0 ->
                IO.puts("Sending to negative ring")
            n == 0 ->
                IO.puts("Sending to zero ring")
            n > 0 && (Integer.mod(n, 2) == 0) ->
                IO.puts("send to even ring")
            n > 0 && (Integer.mod(n, 2) != 0) ->
                IO.puts("Send to odd ring")
        end
    end
end
