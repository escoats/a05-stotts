# Implementation here!
defmodule FourRings do
    def start(n, h) do
        IO.puts("N = #{n}")
        IO.puts("H = #{h}")

        # spawn & wire together nodes


        # prompt for input
        input = IO.gets("Enter a number: \n")
        {num, _} = Integer.parse(input)
        cond do
            input == "done" ->
                # TODO: stop accepting new input
                IO.puts("Shutting down")
            num < 0 ->
                IO.puts("Sending to negative ring")
            num == 0 ->
                IO.puts("Sending to zero ring")
            num > 0 && (Integer.mod(num, 2) == 0) ->
                IO.puts("send to even ring")
            num > 0 && (Integer.mod(num, 2) != 0) ->
                IO.puts("Send to odd ring")
        end
    end

    def neg_gatekeeper(node_1_PID) do
        receive do
            {token_id, ring_id, orig_input, h} ->
            current_val = orig_input
            remaining_hops = h
            send(node_1_PID, {token_id, ring_id, orig_input, current_val, remaining_hops})

            # wait for completion message from same token
            receive do
              {:complete, ^token_id, orig_input, final_val} ->
                IO.puts("Token #{token_id} completed in ring #{ring_id}: #{orig_input} -> #{final_val}")
            end
            neg_gatekeeper(node_1_PID)

            "done" ->
                send(node_1_PID, "done")
                :ok
        end
    end

    def neg_node(next_PID) do
        receive do
            {token_id, ring_id, orig_input, current_val, remaining_hops} ->
                new_val = current_val*3 + 1
                hops_left = remaining_hops - 1

                if hops_left > 0 do
                    # forward to next node
                    send(next_PID, {token_id, ring_id, orig_input, new_val, hops_left})
                else
                    IO.puts("Report completion to manager process")
                end
        end
        neg_node(next_PID)
    end
end
